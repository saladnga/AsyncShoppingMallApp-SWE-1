package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.broker.Message;
import com.common.dto.item.ItemEditRequest;
import com.common.dto.item.ItemUploadRequest;
import com.common.dto.item.ItemSearchRequest;
import com.common.dto.item.ItemLikeRequest;
import com.entities.Item;
import com.entities.LikeRecord;
import com.entities.ItemRanking;
import com.repository.ItemRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * ItemManagement subsystem
 * Handles:
 * - Customer Browsing
 * - Likes
 * - Item uploads/edits/removals (Admin)
 * - Ranking computations
 * - Purchase trigger (PurchaseManager is inside OrderManagement, but we
 * coordinate)
 */

public class ItemManagement implements Subsystems {

    private AsyncMessageBroker broker;

    private final ItemRepository repo;

    // Temporary stores for likes kept locally where no repo exists yet
    private Map<Integer, LikeRecord> likeDB = new HashMap<>();
    private Map<Integer, ItemRanking> rankingDB = new HashMap<>();

    public ItemManagement(ItemRepository repo) {
        this.repo = repo;
    }

    // Listeners (each corresponds to UML manager)
    private final Listener browseListener = this::handleBrowse;
    private final Listener searchListener = this::handleSearch;
    private final Listener uploadListener = this::handleUpload;
    private final Listener editListener = this::handleEdit;
    private final Listener refillListener = this::handleRefill;
    private final Listener removeListener = this::handleRemove;
    private final Listener likeListener = this::handleLike;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;

        // Register listeners
        broker.registerListener(EventType.ITEM_BROWSE_REQUESTED, browseListener);
        broker.registerListener(EventType.ITEM_SEARCH_REQUESTED, searchListener);

        broker.registerListener(EventType.ITEM_UPLOAD_REQUESTED, uploadListener);
        broker.registerListener(EventType.ITEM_EDIT_REQUESTED, editListener);
        broker.registerListener(EventType.ITEM_REFILL_REQUESTED, refillListener);
        broker.registerListener(EventType.ITEM_REMOVE_REQUESTED, removeListener);

        broker.registerListener(EventType.ITEM_LIKE_REQUESTED, likeListener);

        System.out.println("[ItemManagement] Initialized");
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.ITEM_BROWSE_REQUESTED, browseListener);
        broker.unregisterListener(EventType.ITEM_SEARCH_REQUESTED, searchListener);

        broker.unregisterListener(EventType.ITEM_UPLOAD_REQUESTED, uploadListener);
        broker.unregisterListener(EventType.ITEM_EDIT_REQUESTED, editListener);
        broker.unregisterListener(EventType.ITEM_REFILL_REQUESTED, refillListener);
        broker.unregisterListener(EventType.ITEM_REMOVE_REQUESTED, removeListener);

        broker.unregisterListener(EventType.ITEM_LIKE_REQUESTED, likeListener);

        System.out.println("[ItemManagement] Shutdown complete");
    }

    @Override
    public void start() {
    }

    // ================================================================
    // BROWSE MANAGER
    // ================================================================
    private CompletableFuture<Void> handleBrowse(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[ItemManagement] Browsing all items...");

            List<Item> items = repo.findAll();

            // Sort by like count (descending) - most liked items first
            items.sort((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()));

            broker.publish(EventType.ITEM_LIST_RETURNED, items);
        });
    }

    private CompletableFuture<Void> handleSearch(Message message) {
        return CompletableFuture.runAsync(() -> {

            Object payload = message.getPayload();
            String term = null;

            if (payload instanceof ItemSearchRequest req) {
                term = req.getKeyword();
            } else if (payload instanceof String) {
                term = (String) payload;
            }

            System.out.println("[ItemManagement] Searching for: " + (term != null ? term : ""));

            List<Item> results = repo.searchByKeyword(term == null ? "" : term);

            // TC14: Search Empty - handle no results
            if (results == null || results.isEmpty()) {
                System.out.println("[ItemManagement] No matches found for: " + (term != null ? term : ""));
                broker.publish(EventType.ITEM_LIST_RETURNED, List.of());
            } else {
                broker.publish(EventType.ITEM_LIST_RETURNED, results);
            }
        });
    }

    // ================================================================
    // ADMIN MANAGER (Upload/Edit/Remove)
    // ================================================================
    private CompletableFuture<Void> handleUpload(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[ItemManagement] Uploading item...");

            Object payload = message.getPayload();
            if (!(payload instanceof ItemUploadRequest req)) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid upload payload");
                return;
            }

            // Invalid Item Upload - validate negative numbers
            if (req.getPrice() < 0) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid values: Price cannot be negative");
                return;
            }

            if (req.getStock() < 0) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid values: Stock cannot be negative");
                return;
            }

            if (req.getName() == null || req.getName().isBlank()) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid values: Item name is required");
                return;
            }

            try {
                Item item = new Item(0, req.getName(), req.getDescription(), req.getPrice(), req.getStock(), 0);
                int id = repo.insert(item);
                if (id > 0) {
                    item.setId(id);
                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, item);
                    System.out.println("[ItemManagement] Item uploaded: " + item.getName() + " (ID: " + id + ")");
                } else {
                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Upload failed: Database error");
                }
            } catch (Exception e) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Upload failed: " + e.getMessage());
            }
        });
    }

    private CompletableFuture<Void> handleEdit(Message message) {
        return CompletableFuture.runAsync(() -> {
            Object payload = message.getPayload();
            if (!(payload instanceof ItemEditRequest req)) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid edit payload");
                return;
            }

            Item item = repo.findById(req.getItemId());
            if (item == null) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Item edit failed: ID not found");
                return;
            }

            if (req.getNewName() != null && !req.getNewName().isBlank())
                item.setName(req.getNewName());

            if (req.getNewDescription() != null)
                item.setDescription(req.getNewDescription());

            if (req.getNewPrice() != null) {
                item.setPrice(req.getNewPrice());
            }

            if (req.getNewStock() != null) {
                item.setStockQuantity(req.getNewStock());
                repo.updateStock(item.getId(), req.getNewStock());
            }

            repo.update(item);
            broker.publish(EventType.ITEM_UPDATE_SUCCESS, item);
            System.out.println("[ItemManagement] Item edited");
        });
    }

    private CompletableFuture<Void> handleRefill(Message message) {
        return CompletableFuture.runAsync(() -> {
            Object payload = message.getPayload();
            if (!(payload instanceof ItemEditRequest req)) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid refill payload");
                return;
            }

            Item item = repo.findById(req.getItemId());
            if (item == null) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Refill failed: ID not found");
                return;
            }

            if (req.getNewStock() != null) {
                int newStock = req.getNewStock();
                repo.updateStock(item.getId(), newStock);
                item.setStockQuantity(newStock);
            }

            broker.publish(EventType.ITEM_UPDATE_SUCCESS, item);
            System.out.println("[ItemManagement] Item refilled");
        });
    }

    private CompletableFuture<Void> handleRemove(Message message) {
        return CompletableFuture.runAsync(() -> {

            int itemId = (Integer) message.getPayload();

            Item existing = repo.findById(itemId);
            if (existing != null) {
                repo.delete(itemId);
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Item removed");
            } else {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Remove failed: Item not found");
            }
        });
    }

    // ================================================================
    // LIKE MANAGER
    // ================================================================
    private CompletableFuture<Void> handleLike(Message message) {
        return CompletableFuture.runAsync(() -> {
            Object payload = message.getPayload();

            int userId = -1;
            int itemId = -1;
            
            // Handle both ItemLikeRequest and Integer for backward compatibility
            if (payload instanceof com.common.dto.item.ItemLikeRequest req) {
                userId = req.getUserId();
                itemId = req.getItemId();
            } else if (payload instanceof Integer) {
                itemId = (Integer) payload;
                // Legacy support - if only itemId provided, we can't check user
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Like request needs user ID");
                return;
            } else {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Invalid like payload");
                return;
            }

            try {
                // Check if user already liked this item
                if (repo.existsLike(userId, itemId)) {
                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Item already liked");
                    return;
                }

                // Insert like record and increment count
                repo.insertLike(userId, itemId);

                // Get updated item to return with new like count
                Item item = repo.findById(itemId);
                if (item != null) {
                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, item);
                    System.out.println(
                            "[ItemManagement] Item #" + itemId + " liked. New count: " + item.getLikeCount());
                } else {
                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Like recorded");
                }
            } catch (Exception e) {
                broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Like failed: " + e.getMessage());
                System.out.println("[ItemManagement] Like error: " + e.getMessage());
            }
        });
    }
}
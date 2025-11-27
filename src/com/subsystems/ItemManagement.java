package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;
import com.entities.Item;
import com.broker.Listener;
import java.util.List;
import java.util.Arrays;

import java.util.concurrent.CompletableFuture;

public class ItemManagement implements Subsystems {
    private AsyncMessageBroker broker;
    private Listener handleRefill = this::handleRefill;
    private Listener handleUpload = this::handleUpload;
    private Listener handleEdit = this::handleEdit;
    private Listener handleSearch = this::handleSearch;
    private Listener handleBrowse = this::handleBrowse;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.ITEM_REFILL_REQUESTED, handleRefill);
        broker.registerListener(EventType.ITEM_UPLOAD_REQUESTED, handleUpload);
        broker.registerListener(EventType.ITEM_BROWSE_REQUESTED, handleBrowse);
        broker.registerListener(EventType.ITEM_EDIT_REQUESTED, handleEdit);
        broker.registerListener(EventType.ITEM_SEARCH_REQUESTED, handleSearch);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.ITEM_REFILL_REQUESTED, handleRefill);
        broker.unregisterListener(EventType.ITEM_UPLOAD_REQUESTED, handleUpload);
        broker.unregisterListener(EventType.ITEM_EDIT_REQUESTED, handleEdit);
    }

    private CompletableFuture<Void> handleRefill(Message message) {
        return CompletableFuture
                .runAsync(() -> {
                    System.out.println("[Item Management] Refilling inventory successfully");
                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, "Inventory refilled");
                });
    }

    private CompletableFuture<Void> handleUpload(Message message) {
        return CompletableFuture
                .runAsync(() -> {
                    System.out.println("[Item Management] Uploading item...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    broker.publish(EventType.ITEM_UPDATE_SUCCESS, message.getPayload());
                    System.out.println("[Item Management] Item uploaded");
                });
    }

    private CompletableFuture<Void> handleEdit(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Item Management] Editing item: " + message.getPayload());
            broker.publish(EventType.ITEM_UPDATE_SUCCESS, message.getPayload());
        });
    };

    private CompletableFuture<Void> handleSearch(Message message) {
        return CompletableFuture.runAsync(() -> {
            String searchTerm = (String) message.getPayload();
            System.out.println("[Item Management] Searching for: " + searchTerm);

            List<Item> results = Arrays.asList(new Item(1, "Laptop", "Gaming laptop", 1299.99, 5, 10));

            broker.publish(EventType.ITEM_LIST_RETURNED, results);
            System.out.println("[Item Management] Search completed");
        });
    };

    private CompletableFuture<Void> handleBrowse(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Item Management] Loading all items..");
            List<Item> items = Arrays.asList(
                    new Item(1, "Laptop", "Gaming laptop", 1299.99, 5, 10),
                    new Item(2, "Laptop A", "Gaming laptop", 1299.99, 5, 10),
                    new Item(2, "Laptop B", "Gaming laptop", 1299.99, 5, 10));

            broker.publish(EventType.ITEM_LIST_RETURNED, items);
            System.out.println("[Item Management] Returned " + items.size() + " items");
        });
    };
}
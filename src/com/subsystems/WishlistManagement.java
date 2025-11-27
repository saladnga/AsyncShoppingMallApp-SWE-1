package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;
import com.entities.Wishlist;
import com.broker.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// Add or remove wishlist entries, handle WISHLIST event
// Cannot add sold-out items, enforce unique constraint, publish success/failure events

public class WishlistManagement implements Subsystems {
    private AsyncMessageBroker broker;
    private Listener handleAddToWishList = this::handleAddToWishList;
    private Listener handleRemoveFromWishList = this::handleRemoveFromWishList;
    private Listener handleViewWishList = this::handleViewWishList;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.WISHLIST_ADD_REQUESTED, handleAddToWishList);
        broker.registerListener(EventType.WISHLIST_REMOVE_REQUESTED, handleRemoveFromWishList);
        broker.registerListener(EventType.WISHLIST_VIEW_REQUESTED, handleViewWishList);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.WISHLIST_ADD_REQUESTED, handleAddToWishList);
        broker.unregisterListener(EventType.WISHLIST_REMOVE_REQUESTED, handleRemoveFromWishList);
        broker.unregisterListener(EventType.WISHLIST_VIEW_REQUESTED, handleViewWishList);
    }

    private CompletableFuture<Void> handleAddToWishList(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Wishlist Management] Adding item to wishlist...");
            broker.publish(EventType.WISHLIST_ADD_SUCCESS, message.getPayload());
        });
    }

    private CompletableFuture<Void> handleRemoveFromWishList(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Wishlist Management] Removing item to wishlist...");
            broker.publish(EventType.WISHLIST_REMOVE_SUCCESS, message.getPayload());
        });
    }

    private CompletableFuture<Void> handleViewWishList(Message message) {
        return CompletableFuture.runAsync(() -> {
            int userId = (Integer) message.getPayload();
            System.out.println("[Wishlist Management] Loading wishlist for user: " + userId);

            List<Wishlist> wishlist = Arrays.asList(
                    new Wishlist(1, userId, 1, 2, System.currentTimeMillis()),
                    new Wishlist(2, userId, 3, 1, System.currentTimeMillis() - 3600000));

            broker.publish(EventType.WISHLIST_DETAILS_RETURNED, wishlist);
            System.out.println("[Wishlist Management] Wishlist loaded");
        });
    }
}
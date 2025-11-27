package com.entities;

/**
 * Represents an entry in a customer's wishlist.
 *
 * Business Rules:
 * - Each (customerId, itemId) pair must be unique.
 * - quantity should reflect the desired number of items.
 * - addedAt tracks when the item was added (epoch milliseconds).
 */

public class Wishlist {
    private int id;
    private int customerId; // FK to users(id)
    private int itemId; // FK to items(id)
    private int quantity;
    private long addedAt;

    public Wishlist() {

    }

    public Wishlist(int id, int customerId, int itemId, int quantity, long addedAt) {
        this.id = id;
        this.customerId = customerId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.addedAt = addedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public void increaseQuantity(int amount) {
        if (amount > 0) {
            this.quantity += amount;
        }
    }

    public void decreaseQuantity(int amount) {
        if (amount > 0 && this.quantity - amount >= 0) {
            this.quantity -= amount;
        }
    }
}

package com.entities;

/**
 * Represents an item included in an order.
 *
 * Each OrderItem links an order to a specific item and records:
 * - the quantity purchased
 * - the price of the item at the time of purchase
 * - the order it belongs to
 */

public class OrderItem {
    private int id;
    private int orderId;
    private int itemId;
    private int quantity;
    private double priceAtPurchase;

    public OrderItem() {

    }

    public OrderItem(int id, int orderId, int itemId, int quantity, double priceAtPurchase) {
        this.id = id;
        this.orderId = orderId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
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

    public double getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }

    public double getTotalPrice() {
        return quantity * priceAtPurchase;
    }
}

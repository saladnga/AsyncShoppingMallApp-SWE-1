package com.entities;

/**
 * Represents a customer's order in the system.
 *
 * Order Flow:
 * 1. Create an order
 * 2. Publish PAYMENT_AUTHORIZATION_REQUESTED event
 * 3. On success:
 * - Publish ORDER_CONFIRMED event
 * - Publish EMAIL_RECEIPT_REQUESTED event
 */

public class Order {
    public enum OrderStatus {
        PLACE,
        SHIPPED,
        DELIVERED,
        CANCELED
    }

    private int id;
    private int customerId; // FK to users(id)
    private long orderDate;
    private OrderStatus status; // Track lifecycle
    private double totalAmount;
    private String billingAddress;

    public Order() {

    }

    public Order(int id, int customerId, long orderDate, OrderStatus status, double totalAmount,
            String billingAddress) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
        this.billingAddress = billingAddress;
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

    public long getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(long orderDate) {
        this.orderDate = orderDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public boolean canCancel() {
        return status == OrderStatus.PLACE;
    }
}

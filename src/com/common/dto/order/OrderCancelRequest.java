package com.common.dto.order;

public class OrderCancelRequest {
    private final int orderId;
    private final int userId;

    public OrderCancelRequest(int orderId, int userId) {
        this.orderId = orderId;
        this.userId = userId;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "OrderCancelRequest{" +
                "orderId=" + orderId +
                ", userId=" + userId +
                '}';
    }
}
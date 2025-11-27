package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import com.broker.Listener;
import com.entities.Order;
import com.entities.Order.OrderStatus;
import java.util.List;

// Process ORDER_CREATED_REQUEST and PURCHASE flows, manage order history, cancellation
// Reserve inventory, create order record, publish PAYMENT_AUTHORIZATION_REQUESTED
// On PAYMENT_AUTHORIZED: mark order confirmed and publish EMAIL_RECEIPT_REQUESTED
// On cancellation: publish REFUND_PROCESS_REQUESTED, update order status

public class OrderManagement implements Subsystems {
    private AsyncMessageBroker broker;
    private Listener handleOrderCancel = this::handleOrderCancel;
    private Listener handleOrderCreated = this::handleOrderCreated;
    private Listener handlePaymentAuthorized = this::handlePaymentAuthorized;
    private Listener handlePurchase = this::handlePurchase;
    private Listener handleOrderHistory = this::handleOrderHistory;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.ORDER_CREATED_REQUESTED, handleOrderCreated);
        broker.registerListener(EventType.ORDER_CANCEL_REQUESTED, handleOrderCancel);
        broker.registerListener(EventType.PAYMENT_AUTHORIZED, handlePaymentAuthorized);
        broker.registerListener(EventType.PURCHASE_REQUESTED, handlePurchase);
        broker.registerListener(EventType.ORDER_HISTORY_REQUESTED, handleOrderHistory);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.ORDER_CREATED_REQUESTED, handleOrderCreated);
        broker.registerListener(EventType.ORDER_CANCEL_REQUESTED, handleOrderCancel);
        broker.unregisterListener(EventType.PAYMENT_AUTHORIZED, handlePaymentAuthorized);
    }

    private CompletableFuture<Void> handleOrderCreated(Message message) {
        // Async order creation flow
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Order Management] Creating order...");

            // Example
            Order order = new Order(1, 1, System.currentTimeMillis(), Order.OrderStatus.PLACE, 99.99, "E-35");

            broker.publish(EventType.PURCHASE_REQUESTED, order);
            System.out.println("[Order Management] Order created - order: " + order.getId());
        });
    }

    private CompletableFuture<Void> handleOrderCancel(Message message) {
        return CompletableFuture
                .runAsync(() -> {
                    System.out.println("[Order Management] Processing order cancellation, processing refund");
                    broker.publish(EventType.REFUND_PROCESS_REQUESTED, message.getPayload());
                });
    }

    private CompletableFuture<Void> handlePaymentAuthorized(Message message) {
        return CompletableFuture
                .runAsync(() -> {
                    System.out.println("[Order Management] Payment authorized, confirming order...");
                    broker.publish(EventType.EMAIL_RECEIPT_REQUESTED, message.getPayload());
                });
    }

    private CompletableFuture<Void> handlePurchase(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Order Management] Processing purchase:" + message.getPayload());

            Order order = new Order(1, 1, System.currentTimeMillis(), Order.OrderStatus.PLACE, 99.99,
                    "123 Main Street");

            broker.publish(EventType.PAYMENT_AUTHORIZATION_REQUESTED, order);
            System.out.println("[Order Management] Order created, requesting payment");
        });
    }

    private CompletableFuture<Void> handleOrderHistory(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Order Management] Loading order history...");

            List<Order> orders = Arrays.asList(
                    new Order(1, 1, System.currentTimeMillis(), OrderStatus.DELIVERED, 99.99, "Main street"),
                    new Order(2, 1, System.currentTimeMillis() - 86400, OrderStatus.DELIVERED, 99.99, "Main street"));

            broker.publish(EventType.ORDER_HISTORY_RETURNED, orders);
            System.out.println("[Order Management] Order history loaded");
        });
    }
}

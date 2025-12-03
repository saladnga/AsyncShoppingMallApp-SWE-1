package com.subsystems;

import com.broker.*;
import com.entities.Order;
import com.entities.Order.OrderStatus;
import com.entities.OrderItem;
import com.common.dto.order.OrderCreateRequest;
import com.managers.order.CreateOrderManager;
import com.repository.OrderRepository;
import com.repository.OrderItemRepository;
import com.repository.ItemRepository;
import com.entities.Item;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class OrderManagement implements Subsystems {

    private AsyncMessageBroker broker;
    private final CreateOrderManager createOrderManager;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final ItemRepository itemRepo;

    public OrderManagement(CreateOrderManager createOrderManager, OrderRepository orderRepo,
            OrderItemRepository orderItemRepo, ItemRepository itemRepo) {
        this.createOrderManager = createOrderManager;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.itemRepo = itemRepo;
    }

    private final Listener handleOrderCreate = this::onOrderCreate;
    private final Listener handleOrderCancel = this::onCancel;
    private final Listener handleAuthSuccess = this::onPaymentAuthorized;
    private final Listener handleAuthDenied = this::onPaymentDenied;
    private final Listener handlePurchase = this::onPurchase;
    private final Listener handleHistory = this::onHistoryRequest;
    private final Listener handleStatusUpdate = this::onStatusUpdate;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;

        broker.registerListener(EventType.ORDER_CREATED_REQUESTED, handleOrderCreate);
        broker.registerListener(EventType.ORDER_CANCEL_REQUESTED, handleOrderCancel);
        broker.registerListener(EventType.PAYMENT_AUTHORIZED, handleAuthSuccess);
        broker.registerListener(EventType.PAYMENT_DENIED, handleAuthDenied);
        broker.registerListener(EventType.PURCHASE_REQUESTED, handlePurchase);
        broker.registerListener(EventType.ORDER_HISTORY_REQUESTED, handleHistory);
        broker.registerListener(EventType.ORDER_STATUS_UPDATE_REQUESTED, handleStatusUpdate);

        System.out.println("[OrderManagement] Initialized.");
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.ORDER_CREATED_REQUESTED, handleOrderCreate);
        broker.unregisterListener(EventType.ORDER_CANCEL_REQUESTED, handleOrderCancel);
        broker.unregisterListener(EventType.PAYMENT_AUTHORIZED, handleAuthSuccess);
        broker.unregisterListener(EventType.PAYMENT_DENIED, handleAuthDenied);
        broker.unregisterListener(EventType.PURCHASE_REQUESTED, handlePurchase);
        broker.unregisterListener(EventType.ORDER_HISTORY_REQUESTED, handleHistory);
        broker.unregisterListener(EventType.ORDER_STATUS_UPDATE_REQUESTED, handleStatusUpdate);

        System.out.println("[OrderManagement] Shutdown complete.");
    }

    // ============================================================
    // ORDER_CREATED_REQUESTED
    // ============================================================
    private CompletableFuture<Void> onOrderCreate(Message m) {

        return CompletableFuture.runAsync(() -> {
            Order order = (Order) m.getPayload();

            System.out.println("[OrderManagement] Creating order " + order.getId());

            // publish correctly with broker.publish(...)
            broker.publish(EventType.PURCHASE_REQUESTED, order);

            System.out.println("[OrderManagement] Order created.");
        });
    }

    // ============================================================
    // PURCHASE_REQUESTED
    // Handle OrderCreateRequest - stock should be checked before
    // payment
    // ============================================================
    private CompletableFuture<Void> onPurchase(Message m) {

        return CompletableFuture.runAsync(() -> {
            Object payload = m.getPayload();

            // Handle both OrderCreateRequest and Order for backward compatibility
            if (payload instanceof OrderCreateRequest req) {
                // Stock validation should happen before payment
                // This subsystem assumes stock is already validated
                System.out.println("[OrderManagement] Purchase requested for user " + req.getUserId());

                // Publish payment authorization request
                // Note: In a real implementation, we'd need to create the Order first
                // and then request payment. For now, we pass the request through.
                broker.publish(EventType.PAYMENT_AUTHORIZATION_REQUESTED, req);

            } else if (payload instanceof Order order) {
                System.out.println("[OrderManagement] Purchase started for order " + order.getId());
                broker.publish(EventType.PAYMENT_AUTHORIZATION_REQUESTED, order);
            } else {
                System.out.println("[OrderManagement] Invalid purchase payload type");
            }
        });
    }

    // ============================================================
    // PAYMENT_AUTHORIZED
    // Purchase Success - order confirmed after payment
    // ============================================================
    private CompletableFuture<Void> onPaymentAuthorized(Message m) {

        return CompletableFuture.runAsync(() -> {
            Object payload = m.getPayload();

            Order order = null;

            // Handle OrderCreateRequest - create the order now that payment is authorized
            if (payload instanceof OrderCreateRequest req) {
                try {
                    System.out.println(
                            "[OrderManagement] Payment authorized for user " + req.getUserId() + ", creating order...");

                    if (req.getItems().isEmpty()) {
                        System.out.println("[OrderManagement] OrderCreateRequest has no items");
                        broker.publish(EventType.ORDER_PAYMENT_FAILED, "No items in order");
                        return;
                    }

                    // Calculate total amount for all items
                    double totalAmount = 0.0;
                    for (OrderCreateRequest.OrderItemRequest itemReq : req.getItems()) {
                        com.entities.Item item = itemRepo.findById(itemReq.getItemId());
                        if (item == null) {
                            throw new Exception("Item " + itemReq.getItemId() + " not found");
                        }
                        totalAmount += item.getPrice() * itemReq.getQuantity();
                    }

                    // Create a single order with all items
                    // Use the first item to create the order, then add other items as order items
                    OrderCreateRequest.OrderItemRequest firstItem = req.getItems().get(0);

                    // Create order using CreateOrderManager (creates order + first order item)
                    order = createOrderManager.createOrder(
                            req.getUserId(),
                            firstItem.getItemId(),
                            firstItem.getQuantity());

                    // Decrement stock for the first item (since CreateOrderManager no longer does
                    // it)
                    com.entities.Item firstItemEntity = itemRepo.findById(firstItem.getItemId());
                    if (firstItemEntity != null) {
                        int oldStock = firstItemEntity.getStockQuantity();
                        int newStock = oldStock - firstItem.getQuantity();
                        System.out.println("[OrderManagement] Updating stock for item " + firstItem.getItemId() +
                                ": " + oldStock + " -> " + newStock);
                        itemRepo.updateStock(firstItem.getItemId(), newStock);

                        // Verify the update
                        Item updatedItem = itemRepo.findById(firstItem.getItemId());
                        System.out.println("[OrderManagement] Stock after update: " +
                                (updatedItem != null ? updatedItem.getStockQuantity() : "item not found"));
                    }

                    // Add remaining items to the same order
                    for (int i = 1; i < req.getItems().size(); i++) {
                        OrderCreateRequest.OrderItemRequest itemReq = req.getItems().get(i);
                        com.entities.Item item = itemRepo.findById(itemReq.getItemId());
                        if (item == null) {
                            System.out
                                    .println("[OrderManagement] Item " + itemReq.getItemId() + " not found, skipping");
                            continue;
                        }

                        // Check stock
                        if (item.getStockQuantity() < itemReq.getQuantity()) {
                            System.out.println("[OrderManagement] Insufficient stock for item " + itemReq.getItemId()
                                    + ", skipping");
                            continue;
                        }

                        // Create order item
                        OrderItem orderItem = new OrderItem(0, order.getId(), itemReq.getItemId(),
                                itemReq.getQuantity(), item.getPrice());
                        orderItemRepo.insert(orderItem);

                        // Reduce stock
                        int oldStock = item.getStockQuantity();
                        int newStock = oldStock - itemReq.getQuantity();
                        System.out.println("[OrderManagement] Updating stock for item " + itemReq.getItemId() +
                                ": " + oldStock + " -> " + newStock);
                        itemRepo.updateStock(itemReq.getItemId(), newStock);
                    }

                    // Update order with shipping address if provided
                    if (req.getShippingAddress() != null && !req.getShippingAddress().isBlank()) {
                        order.setBillingAddress(req.getShippingAddress());
                    }

                    // Update total amount in order (sum of all items) and database
                    if (totalAmount != order.getTotalAmount()) {
                        order.setTotalAmount(totalAmount);
                        // Update in database
                        if (orderRepo instanceof com.repository.SQLiteOrderRepository) {
                            ((com.repository.SQLiteOrderRepository) orderRepo).updateTotal(order.getId(), totalAmount);
                        }
                    }

                } catch (Exception e) {
                    System.out.println("[OrderManagement] Failed to create order after payment: " + e.getMessage());
                    broker.publish(EventType.ORDER_PAYMENT_FAILED, "Order creation failed: " + e.getMessage());
                    return;
                }
            } else if (payload instanceof Order) {
                order = (Order) payload;
            } else if (payload instanceof com.common.dto.payment.PaymentAuthorizeRequest req) {
                // If we receive PaymentAuthorizeRequest, we need to get/create the order
                System.out.println("[OrderManagement] Payment authorized for user " + req.getUserId());
                // Can't create order without item info, just publish confirmation
                broker.publish(EventType.ORDER_CONFIRMED, req);
                return;
            } else {
                System.out.println("[OrderManagement] Payment authorized with unknown payload type: "
                        + payload.getClass().getName());
                return;
            }

            if (order != null) {
                // TC18: Order is already PLACED status from CreateOrderManager
                System.out.println("[OrderManagement] Payment authorized – confirming order " + order.getId() + "...");

                broker.publish(EventType.ORDER_CONFIRMED, order);
                broker.publish(EventType.EMAIL_RECEIPT_REQUESTED, order);
                broker.publish(EventType.SHIPPING_REQUESTED, order.getId());
            }
        });
    }

    // ============================================================
    // PAYMENT_DENIED
    // Payment Processing Timeout/Failure and Payment Rejected
    // ============================================================
    private CompletableFuture<Void> onPaymentDenied(Message m) {

        return CompletableFuture.runAsync(() -> {
            Object payload = m.getPayload();

            // Handle payment denial/timeout
            String reason = "Payment Authorization Failed";
            if (payload instanceof String) {
                reason = (String) payload;
            } else if (payload instanceof com.common.dto.payment.PaymentAuthorizeRequest req) {
                reason = "Payment Failed: Authorization denied";
                System.out.println("[OrderManagement] Payment denied for user " + req.getUserId() + ", amount $"
                        + req.getAmount());
            } else if (payload instanceof Order order) {
                reason = "Payment Failed: Order #" + order.getId() + " could not be processed";
                System.out.println("[OrderManagement] Payment denied for order " + order.getId());
                // Order status should NOT be set to PLACED
                // Order remains in its previous state (e.g., PENDING)
            }

            System.out.println("[OrderManagement] " + reason);
            broker.publish(EventType.ORDER_PAYMENT_FAILED, reason);
        });
    }

    // ============================================================
    // ORDER_CANCEL_REQUESTED
    // Cancel Eligible/Ineligible Order
    // ============================================================
    private CompletableFuture<Void> onCancel(Message m) {

        return CompletableFuture.runAsync(() -> {
            Object payload = m.getPayload();
            if (!(payload instanceof Order order)) {
                System.out.println("[OrderManagement] Invalid cancel request payload");
                return;
            }

            System.out.println("[OrderManagement] Cancelling order " + order.getId());

            // Check if order is cancellable
            // Only PLACED orders can be cancelled, not SHIPPED or DELIVERED
            if (order.getStatus() == OrderStatus.PLACED) {
                // Cancel eligible order
                order.setStatus(OrderStatus.CANCELED);
                System.out.println("[OrderManagement] Order " + order.getId() + " cancelled successfully");

                broker.publish(EventType.REFUND_PROCESS_REQUESTED, order);
                broker.publish(EventType.ORDER_CANCEL_SUCCESS, order);
            } else {
                // Cancel ineligible order
                String errorMsg = "Cannot cancel order: Order status is " + order.getStatus() +
                        ". Only PLACED orders can be cancelled.";
                System.out.println("[OrderManagement] " + errorMsg);
                broker.publish(EventType.ORDER_CANCEL_FAILED, errorMsg);
            }
        });
    }

    // ============================================================
    // ORDER_HISTORY_REQUESTED
    // ============================================================
    private CompletableFuture<Void> onHistoryRequest(Message message) {

        return CompletableFuture.runAsync(() -> {
            Object payload = message.getPayload();

            // Payload should be the customer/user ID
            int customerId = -1;
            if (payload instanceof Integer) {
                customerId = (Integer) payload;
            } else if (payload instanceof com.entities.User) {
                customerId = ((com.entities.User) payload).getId();
            } else {
                System.out.println("[OrderManagement] Invalid payload for order history: " + payload);
                broker.publish(EventType.ORDER_HISTORY_RETURNED, new ArrayList<>());
                return;
            }

            // Query orders from database
            List<Order> orders = orderRepo.findByCustomer(customerId);

            broker.publish(EventType.ORDER_HISTORY_RETURNED, orders);
            System.out.println("[OrderManagement] Order history returned for customer " + customerId + ": "
                    + orders.size() + " orders");
        });
    }

    // ============================================================
    // ORDER_STATUS_UPDATE_REQUESTED
    // ============================================================
    private CompletableFuture<Void> onStatusUpdate(Message message) {
        return CompletableFuture.runAsync(() -> {
            Object payload = message.getPayload();

            if (!(payload instanceof Order order)) {
                System.out.println("[OrderManagement] Invalid payload for status update: " + payload);
                return;
            }

            // Update order status in database
            orderRepo.updateStatus(order.getId(), order.getStatus());
            System.out.println("[OrderManagement] Order " + order.getId() + " status updated to " + order.getStatus());

            // Publish confirmation
            broker.publish(EventType.ORDER_STATUS_RETURNED, order);
        });
    }
}
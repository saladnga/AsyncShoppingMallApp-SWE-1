package com.managers.order;

import com.entities.Item;
import com.entities.Order;
import com.entities.OrderItem;
import com.repository.ItemRepository;
import com.repository.OrderRepository;
import com.repository.OrderItemRepository;

public class CreateOrderManager {

    private final ItemRepository itemRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;

    public CreateOrderManager(ItemRepository itemRepo,
            OrderRepository orderRepo,
            OrderItemRepository orderItemRepo) {
        this.itemRepo = itemRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
    }

    public Order createOrder(int userId, int itemId, int quantity) throws Exception {

        if (quantity <= 0)
            throw new Exception("Quantity must be greater than 0");

        // 1. Load item
        Item item = itemRepo.findById(itemId);
        if (item == null)
            throw new Exception("Item does not exist");

        if (item.getStockQuantity() < quantity)
            throw new Exception("Not enough stock");

        // 2. Calculate total
        double totalPrice = item.getPrice() * quantity;

        // 3. Create Order (status: PLACE)
        Order order = new Order(
                0,
                userId,
                System.currentTimeMillis(),
                Order.OrderStatus.PLACED,
                totalPrice,
                "Pending Address");

        int orderId = orderRepo.insert(order);
        order.setId(orderId);

        // 4. Create OrderItem
        OrderItem orderItem = new OrderItem(0, orderId, itemId, quantity, item.getPrice());
        orderItemRepo.insert(orderItem);

        // Note: Stock will be decremented after payment authorization in OrderManagement

        return order;
    }
}
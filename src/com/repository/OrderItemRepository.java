package com.repository;

import com.entities.OrderItem;
import java.util.List;

public interface OrderItemRepository {

    /** Insert item into table */
    void insert(OrderItem item);

    /** Load all items belonging to an order */
    List<OrderItem> findByOrderId(int orderId);
}
package com.repository;

import com.entities.Order;
import java.util.List;

public interface OrderRepository {

    /**
     * Insert a new order and return generated ID.
     */
    int insert(Order order);

    /**
     * Load order basic info.
     */
    Order findById(int orderId);

    /**
     * Load all orders for a specific customer.
     */
    List<Order> findByCustomer(int customerId);

    /**
     * Update the status (PLACED, CANCELLED, DELIVERED).
     */
    void updateStatus(int orderId, Order.OrderStatus status);

    // =============================
    // REPORTING
    // =============================

    /**
     * Compute total sales for a given period.
     */
    double computeTotalSales(long start, long end);

    /**
     * Count number of orders in a period.
     */
    int countOrders(long start, long end);
}
package com.repository;

import com.common.Database;
import com.entities.Order;
import com.entities.Order.OrderStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SQLiteOrderRepository implements OrderRepository {

    private final Database db;

    public SQLiteOrderRepository(Database db) {
        this.db = db;
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        return new Order(
                rs.getInt("id"),
                rs.getInt("customer_id"),
                rs.getLong("order_date"),
                parseStatus(rs.getString("status")),
                rs.getDouble("total_amount"),
                rs.getString("billing_address"));
    }

    private OrderStatus parseStatus(String statusStr) {
        if (statusStr == null)
            return OrderStatus.PLACED;
        try {
            return OrderStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            return OrderStatus.PLACED;
        }
    }

    @Override
    public int insert(Order order) {
        String sql = "INSERT INTO orders(customer_id, order_date, status, total_amount, billing_address) " +
                "VALUES (?, ?, ?, ?, ?)";
        return db.executeInsertReturnId(sql,
                order.getCustomerId(),
                order.getOrderDate(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getBillingAddress());
    }

    @Override
    public Order findById(int orderId) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        return db.queryOne(sql, rs -> mapRow(rs), orderId);
    }

    @Override
    public List<Order> findByCustomer(int customerId) {
        String sql = "SELECT * FROM orders WHERE customer_id = ? ORDER BY order_date DESC";
        return db.queryList(sql, rs -> mapRow(rs), customerId);
    }

    @Override
    public void updateStatus(int orderId, OrderStatus status) {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        db.executeUpdate(sql, status.name(), orderId);
    }

    /**
     * Update order total amount
     */
    public void updateTotal(int orderId, double totalAmount) {
        String sql = "UPDATE orders SET total_amount = ? WHERE id = ?";
        db.executeUpdate(sql, totalAmount, orderId);
    }

    @Override
    public double computeTotalSales(long start, long end) {
        String sql = "SELECT COALESCE(SUM(total_amount), 0) FROM orders " +
                "WHERE order_date >= ? AND order_date <= ? AND status != 'CANCELED'";
        Double result = db.queryOne(sql, rs -> rs.getDouble(1), start, end);
        return result != null ? result : 0.0;
    }

    @Override
    public int countOrders(long start, long end) {
        String sql = "SELECT COUNT(*) FROM orders " +
                "WHERE order_date >= ? AND order_date <= ? AND status != 'CANCELED'";
        Integer result = db.queryOne(sql, rs -> rs.getInt(1), start, end);
        return result != null ? result : 0;
    }
}
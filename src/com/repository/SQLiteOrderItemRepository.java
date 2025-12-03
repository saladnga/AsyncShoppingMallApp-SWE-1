package com.repository;

import com.common.Database;
import com.entities.OrderItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SQLiteOrderItemRepository implements OrderItemRepository {

    private final Database db;

    public SQLiteOrderItemRepository(Database db) {
        this.db = db;
    }

    private OrderItem mapRow(ResultSet rs) throws SQLException {
        return new OrderItem(
                rs.getInt("id"),
                rs.getInt("order_id"),
                rs.getInt("item_id"),
                rs.getInt("quantity"),
                rs.getDouble("unit_price"));
    }

    @Override
    public void insert(OrderItem item) {
        String sql = "INSERT INTO order_items(order_id, item_id, quantity, unit_price, sub_total) " +
                "VALUES (?, ?, ?, ?, ?)";
        double subTotal = item.getQuantity() * item.getPriceAtPurchase();
        db.executeUpdate(sql,
                item.getOrderId(),
                item.getItemId(),
                item.getQuantity(),
                item.getPriceAtPurchase(),
                subTotal);
    }

    @Override
    public List<OrderItem> findByOrderId(int orderId) {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        return db.queryList(sql, rs -> mapRow(rs), orderId);
    }
}
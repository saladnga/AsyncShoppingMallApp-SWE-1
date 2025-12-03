package com.repository;

import com.common.Database;
import com.entities.UserMessage;
import com.entities.Conversation;

import java.sql.*;
import java.util.*;

public class SQLiteMessageRepository extends MessageRepository {

    private final Database db;

    public SQLiteMessageRepository(Database database) {
        this.db = database;
    }

    @Override
    public synchronized UserMessage save(UserMessage msg) {
        // First, handle conversation creation/retrieval
        int conversationId = getOrCreateConversation(msg);

        String sql = """
                INSERT INTO messages(conversation_id, user_id, role, content, is_read, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        long timestamp = System.currentTimeMillis();
        int isRead = msg.getStatus() == UserMessage.MessageStatus.READ ? 1 : 0;
        String role = determineUserRole(msg.getSenderId());

        int id = db.executeInsertReturnId(sql,
                conversationId,
                msg.getSenderId(),
                role,
                msg.getContent(),
                isRead,
                timestamp / 1000 // Convert to seconds for SQLite
        );

        msg.setId(id);
        msg.setTimeStamp(timestamp);
        return msg;
    }

    @Override
    public synchronized List<UserMessage> getConversation(int userId, int staffId) {
        String sql = """
                SELECT m.id, m.conversation_id, m.user_id, m.role, m.content,
                       m.is_read, m.created_at * 1000 as timestamp_ms,
                       c.customer_id, c.subject
                FROM messages m
                JOIN conversations c ON m.conversation_id = c.id
                WHERE c.customer_id = ? OR c.customer_id = ?
                ORDER BY m.created_at ASC
                """;

        return db.queryList(sql, this::mapMessage, userId, staffId);
    }

    @Override
    public synchronized List<UserMessage> getUnreadMessages(int userId, int staffId) {
        String sql = """
                SELECT m.id, m.conversation_id, m.user_id, m.role, m.content,
                       m.is_read, m.created_at * 1000 as timestamp_ms,
                       c.customer_id, c.subject
                FROM messages m
                JOIN conversations c ON m.conversation_id = c.id
                WHERE c.customer_id = ? AND m.user_id != ? AND m.is_read = 0
                ORDER BY m.created_at DESC
                """;

        return db.queryList(sql, this::mapMessage, userId, staffId);
    }

    @Override
    public synchronized void markRead(int userId, int staffId) {
        String sql = """
                UPDATE messages
                SET is_read = 1
                WHERE conversation_id IN (
                    SELECT id FROM conversations WHERE customer_id = ?
                ) AND user_id = ? AND is_read = 0
                """;

        db.executeUpdate(sql, userId, userId);
    }

    @Override
    public synchronized List<Integer> getConversationPartners(int staffId) {
        String sql = """
                SELECT DISTINCT c.customer_id
                FROM conversations c
                JOIN messages m ON c.id = m.conversation_id
                """;

        return db.queryList(sql, rs -> rs.getInt("customer_id"));
    }

    @Override
    public synchronized List<UserMessage> getMessageHistory(int userId) {
        String sql = """
                SELECT m.id, m.conversation_id, m.user_id, m.role, m.content,
                       m.is_read, m.created_at * 1000 as timestamp_ms,
                       c.customer_id, c.subject
                FROM messages m
                JOIN conversations c ON m.conversation_id = c.id
                WHERE c.customer_id = ? OR m.user_id = ?
                ORDER BY m.created_at ASC
                """;

        return db.queryList(sql, this::mapMessage, userId, userId);
    }

    @Override
    public synchronized List<Conversation> getConversationsForUser(int userId) {
        // Check user role
        String userRole = determineUserRole(userId);

        if ("Customer".equals(userRole)) {
            // Customer sees only their own conversations
            String sql = """
                    SELECT c.id, c.customer_id, c.subject, c.created_at,
                           COUNT(CASE WHEN m.is_read = 0 AND m.user_id != ? THEN 1 END) as unread_count,
                           MAX(m.created_at) as last_message_time,
                           (SELECT content FROM messages WHERE conversation_id = c.id
                            ORDER BY created_at DESC LIMIT 1) as last_message
                    FROM conversations c
                    LEFT JOIN messages m ON c.id = m.conversation_id
                    WHERE c.customer_id = ?
                    GROUP BY c.id, c.customer_id, c.subject, c.created_at
                    ORDER BY last_message_time DESC
                    """;

            return db.queryList(sql, rs -> {
                Conversation conv = new Conversation();
                int customerId = rs.getInt("customer_id");
                conv.setCustomerId(customerId);
                conv.setStaffId(userId);
                conv.setCustomerName("Customer " + customerId);
                conv.setStaffName("Staff " + userId);
                conv.setUnreadCount(rs.getInt("unread_count"));
                conv.setLastMessage(rs.getString("last_message"));
                conv.setLastMessageTime(rs.getLong("last_message_time") * 1000);
                return conv;
            }, userId, userId);
        } else {
            // Staff/CEO sees all conversations
            String sql = """
                    SELECT c.id, c.customer_id, c.subject, c.created_at,
                           COUNT(CASE WHEN m.is_read = 0 AND m.role = 'Customer' AND m.is_read = 0 THEN 1 END) as unread_count,
                           MAX(m.created_at) as last_message_time,
                           (SELECT content FROM messages WHERE conversation_id = c.id
                            ORDER BY created_at DESC LIMIT 1) as last_message
                    FROM conversations c
                    LEFT JOIN messages m ON c.id = m.conversation_id
                    GROUP BY c.id, c.customer_id, c.subject, c.created_at
                    ORDER BY last_message_time DESC
                    """;

            return db.queryList(sql, rs -> {
                Conversation conv = new Conversation();
                int customerId = rs.getInt("customer_id");
                conv.setCustomerId(customerId);
                conv.setStaffId(userId);
                conv.setCustomerName("Customer " + customerId);
                conv.setStaffName("Staff " + userId);
                conv.setUnreadCount(rs.getInt("unread_count"));
                conv.setLastMessage(rs.getString("last_message"));
                conv.setLastMessageTime(rs.getLong("last_message_time") * 1000);
                return conv;
            });
        }
    }

    // === HELPER METHODS ===

    private int getOrCreateConversation(UserMessage msg) {
        // For broadcast messages (recipientId = -1), create conversation with first
        // staff member
        int customerId = msg.getRecipientId() == -1 ? msg.getSenderId()
                : Math.min(msg.getSenderId(), msg.getRecipientId());

        String subject = msg.getSubject() != null ? msg.getSubject() : "General Inquiry";

        // Check if conversation exists
        String checkSql = "SELECT id FROM conversations WHERE customer_id = ? AND subject = ?";
        Integer existingId = db.queryOne(checkSql, rs -> rs.getInt("id"), customerId, subject);

        if (existingId != null) {
            return existingId;
        }

        // Create new conversation
        String insertSql = """
                INSERT INTO conversations(customer_id, subject, created_at)
                VALUES (?, ?, strftime('%s','now'))
                """;

        return db.executeInsertReturnId(insertSql, customerId, subject);
    }

    private String determineUserRole(int userId) {
        // Simple role determination - you might want to query users table instead
        String sql = "SELECT role FROM users WHERE id = ?";
        String role = db.queryOne(sql, rs -> rs.getString("role"), userId);
        return role != null ? role : "Customer";
    }

    private UserMessage mapMessage(ResultSet rs) throws SQLException {
        UserMessage msg = new UserMessage();
        msg.setId(rs.getInt("id"));
        msg.setSenderId(rs.getInt("user_id"));
        msg.setRecipientId(rs.getInt("customer_id"));
        msg.setSubject(rs.getString("subject"));
        msg.setContent(rs.getString("content"));
        msg.setTimeStamp(rs.getLong("timestamp_ms"));
        msg.setStatus(rs.getInt("is_read") == 1 ? UserMessage.MessageStatus.READ : UserMessage.MessageStatus.UNREAD);
        return msg;
    }

    // Add these methods to SQLiteMessageRepository class:

    @Override
    public synchronized List<UserMessage> getUnreadMessagesForStaff(int staffId) {
        String sql = """
                SELECT m.id, m.conversation_id, m.user_id, m.role, m.content,
                       m.is_read, m.created_at * 1000 as timestamp_ms,
                       c.customer_id, c.subject
                FROM messages m
                JOIN conversations c ON m.conversation_id = c.id
                WHERE (m.user_id != ? AND m.is_read = 0) OR
                      (c.customer_id != ? AND m.user_id = c.customer_id AND m.is_read = 0)
                ORDER BY m.created_at DESC
                """;

        return db.queryList(sql, this::mapMessage, staffId, staffId);
    }

    @Override
    public synchronized List<UserMessage> getConversationMessages(int userId, int otherUserId) {
        String sql = """
                SELECT m.id, m.conversation_id, m.user_id, m.role, m.content,
                       m.is_read, m.created_at * 1000 as timestamp_ms,
                       c.customer_id, c.subject
                FROM messages m
                JOIN conversations c ON m.conversation_id = c.id
                WHERE (c.customer_id = ? OR c.customer_id = ?) AND
                      (m.user_id = ? OR m.user_id = ?)
                ORDER BY m.created_at ASC
                """;

        return db.queryList(sql, this::mapMessage, userId, otherUserId, userId, otherUserId);
    }

    @Override
    public synchronized void markMessagesAsRead(int userId, int otherUserId) {
        String sql = """
                UPDATE messages
                SET is_read = 1
                WHERE conversation_id IN (
                    SELECT id FROM conversations
                    WHERE customer_id = ? OR customer_id = ?
                ) AND user_id != ? AND is_read = 0
                """;

        db.executeUpdate(sql, userId, otherUserId, userId);
    }

    @Override
    public synchronized void markCustomerMessagesAsReadByStaff(int customerId, int staffId) {
        String sql = """
                UPDATE messages
                SET is_read = 1
                WHERE conversation_id IN (
                    SELECT id FROM conversations WHERE customer_id = ?
                ) AND user_id = ? AND is_read = 0
                """;

        db.executeUpdate(sql, customerId, customerId);
    }

    @Override
    public synchronized List<UserMessage> getRecentMessagesFromCustomer(int customerId, int limit) {
        String sql = """
                SELECT m.id, m.conversation_id, m.user_id, m.role, m.content,
                       m.is_read, m.created_at * 1000 as timestamp_ms,
                       c.customer_id, c.subject
                FROM messages m
                JOIN conversations c ON m.conversation_id = c.id
                WHERE c.customer_id = ? AND m.role = 'Customer'
                ORDER BY m.created_at DESC
                LIMIT ?
                """;

        return db.queryList(sql, this::mapMessage, customerId, limit);
    }
}
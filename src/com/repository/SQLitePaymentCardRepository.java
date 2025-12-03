package com.repository;

import com.common.Database;
import com.entities.PaymentCard;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SQLitePaymentCardRepository implements PaymentCardRepository {

    private final Database db;

    public SQLitePaymentCardRepository(Database database) {
        this.db = database;
    }

    @Override
    public PaymentCard findById(int id) {
        String sql = "SELECT * FROM payment_methods WHERE id = ?";
        return db.queryOne(sql, this::mapRow, id);
    }

    @Override
    public List<PaymentCard> findByUserId(int userId) {
        String sql = "SELECT * FROM payment_methods WHERE user_id = ? ORDER BY created_at DESC";
        return db.queryList(sql, this::mapRow, userId);
    }

    @Override
    public void insert(PaymentCard card) {
        String sql = "INSERT INTO payment_methods(user_id, card_holder_name, card_number, expiry_date, card_type) VALUES (?, ?, ?, ?, ?)";
        
        // Mask the card number for storage (PCI compliance)
        String maskedNumber = maskCardNumber(card.getMaskedCardNumber());
        
        int id = db.executeInsertReturnId(sql, 
            card.getUserId(),
            card.getCardHolderName(),
            maskedNumber,
            card.getExpiryDate(),
            card.getCardType());
        
        // Update the card object with generated ID
        card = new PaymentCard(id, card.getUserId(), card.getCardType(), 
                              maskedNumber, card.getExpiryDate(), 
                              card.getCardHolderName(), card.isDefault());
    }

    @Override
    public void update(PaymentCard card) {
        String sql = "UPDATE payment_methods SET card_holder_name = ?, expiry_date = ?, card_type = ? WHERE id = ?";
        db.executeUpdate(sql, 
            card.getCardHolderName(),
            card.getExpiryDate(), 
            card.getCardType(),
            card.getId());
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM payment_methods WHERE id = ?";
        db.executeUpdate(sql, id);
    }

    @Override
    public PaymentCard findDefaultCard(int userId) {
        // For now, return the first card as default (you can enhance this later)
        List<PaymentCard> cards = findByUserId(userId);
        return cards.isEmpty() ? null : cards.get(0);
    }

    @Override
    public void setDefaultCard(int userId, int cardId) {
        // Implementation for setting default card (can be enhanced)
        // For now, this is a placeholder
        System.out.println("[SQLitePaymentCardRepository] Set default card " + cardId + " for user " + userId);
    }

    @Override
    public boolean isCardExpired(PaymentCard card) {
        // Simple expiry check - you can enhance this
        return false;
    }

    private PaymentCard mapRow(ResultSet rs) throws SQLException {
        return new PaymentCard(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("card_type"),
            rs.getString("card_number"), // Already masked
            rs.getString("expiry_date"),
            rs.getString("card_holder_name"),
            false // Default flag - can be enhanced
        );
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }
        
        // If already masked, return as is
        if (cardNumber.contains("*")) {
            return cardNumber;
        }
        
        // Mask all but last 4 digits
        String digits = cardNumber.replaceAll("\\s+", "");
        if (digits.length() <= 4) {
            return "**** **** **** " + digits;
        }
        
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }
}
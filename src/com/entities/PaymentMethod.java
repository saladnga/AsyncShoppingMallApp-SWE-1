package com.entities;

/**
 * Represents a payment method (e.g., credit or debit card) stored for a user.
 *
 * Security Notes:
 * - Card numbers should be stored in masked format (e.g., "**** **** ****
 * 1234").
 * - Do NOT store CVV codes for PCI compliance.
 * - Consider encrypting sensitive data if storing in a database.
 */

public class PaymentMethod {
    private int id;
    private int userId;
    private String cardHolderName;
    private String cardNumber;
    private String expiryDate;
    private String cardType;

    public PaymentMethod() {

    }

    public PaymentMethod(int id, int userId, String cardHolderName, String cardNumber, String expiryDate,
            String cardType) {
        this.id = id;
        this.userId = userId;
        this.cardHolderName = cardHolderName;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cardType = cardType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getLast4Digit() {
        if (cardNumber == null || cardNumber.length() < 4)
            return "";
        return cardNumber.substring(cardNumber.length() - 4);
    }
}

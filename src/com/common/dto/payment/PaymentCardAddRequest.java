package com.common.dto.payment;

public class PaymentCardAddRequest {
    private final int userId;
    private final String cardHolderName;
    private final String cardNumber;
    private final String expiryDate;
    private final String cardType;

    public PaymentCardAddRequest(int userId, String cardHolderName, String cardNumber, String expiryDate, String cardType) {
        this.userId = userId;
        this.cardHolderName = cardHolderName;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cardType = cardType;
    }

    public int getUserId() { return userId; }
    public String getCardHolderName() { return cardHolderName; }
    public String getCardNumber() { return cardNumber; }
    public String getExpiryDate() { return expiryDate; }
    public String getCardType() { return cardType; }
}
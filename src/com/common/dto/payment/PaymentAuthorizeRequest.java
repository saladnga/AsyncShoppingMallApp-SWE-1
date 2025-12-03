package com.common.dto.payment;

public class PaymentAuthorizeRequest {

    private final int userId;
    private final int cardId;
    private final double amount;
    private final String expiryDate;
    private final String cvv;

    public PaymentAuthorizeRequest(int userId, int cardId, double amount,
            String expiryDate, String cvv) {

        this.userId = userId;
        this.cardId = cardId;
        this.amount = amount;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
    }

    public int getUserId() {
        return userId;
    }

    public int getCardId() {
        return cardId;
    }

    public double getAmount() {
        return amount;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    @Override
    public String toString() {
        return "PaymentAuthorizeRequest{" +
                "userId=" + userId +
                ", cardId=" + cardId +
                ", amount=" + amount +
                ", expiryDate='" + expiryDate + '\'' +
                '}';
    }
}
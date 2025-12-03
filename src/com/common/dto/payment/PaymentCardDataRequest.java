package com.common.dto.payment;

public class PaymentCardDataRequest {
    private final int userId;
    private final String cardType;

    public PaymentCardDataRequest(int userId, String cardType) {
        this.userId = userId;
        this.cardType = cardType;
    }

    public int getUserId() {
        return userId;
    }

    public String getCardType() {
        return cardType;
    }

    @Override
    public String toString() {
        return "PaymentCardDataRequest{userId=" + userId +
                ", cardType='" + cardType + "'}";
    }
}
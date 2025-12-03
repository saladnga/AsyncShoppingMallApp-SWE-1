package com.common.dto.payment;

public class PaymentCardEditRequest {
    private final int cardId;
    private final String newCardHolderName;
    private final String newCardNumber;
    private final String newExpiryDate;

    public PaymentCardEditRequest(int cardId, String newCardHolderName, String newCardNumber, String newExpiryDate) {
        this.cardId = cardId;
        this.newCardHolderName = newCardHolderName;
        this.newCardNumber = newCardNumber;
        this.newExpiryDate = newExpiryDate;
    }

    public int getCardId() { return cardId; }
    public String getNewCardHolderName() { return newCardHolderName; }
    public String getNewCardNumber() { return newCardNumber; }
    public String getNewExpiryDate() { return newExpiryDate; }
}
package com.managers.payment;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.entities.PaymentCard;
import com.entities.PaymentTransaction;

public class PaymentControllerManager {

    private final PaymentCardManager cardManager;
    private final PaymentAuthorizationManager authManager;
    private final PaymentTransactionManager transactionManager;
    private final SendReceiptManager receiptManager;
    private final AsyncMessageBroker broker;

    public PaymentControllerManager(
            PaymentCardManager cardManager,
            PaymentAuthorizationManager authManager,
            PaymentTransactionManager transactionManager,
            SendReceiptManager receiptManager,
            AsyncMessageBroker broker) {

        this.cardManager = cardManager;
        this.authManager = authManager;
        this.transactionManager = transactionManager;
        this.receiptManager = receiptManager;
        this.broker = broker;
    }

    public void processPayment(int userId, double amount) {

        PaymentCard defaultCard = cardManager.getDefaultCard(userId);

        if (defaultCard == null) {
            broker.publish(EventType.PAYMENT_DENIED, "User has no default card");
            return;
        }

        authManager.authorizePayment(userId, defaultCard.getId(), amount);
    }

    /**
     * Called after PAYMENT_AUTHORIZED event
     */
    public void onPaymentAuthorized(PaymentTransaction transaction) {
        transactionManager.updateStatus(transaction, PaymentTransaction.Status.AUTHORIZED);
        receiptManager.sendReceipt(transaction);
    }
}
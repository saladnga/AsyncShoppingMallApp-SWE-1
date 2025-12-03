package com.managers.payment;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.entities.PaymentCard;
import com.entities.PaymentTransaction;
import com.repository.PaymentCardRepository;
import com.repository.PaymentTransactionRepository;

public class PaymentAuthorizationManager {

    private final PaymentCardRepository cardRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final AsyncMessageBroker broker;

    public PaymentAuthorizationManager(
            PaymentCardRepository cardRepo,
            PaymentTransactionRepository transactionRepo,
            AsyncMessageBroker broker) {

        this.cardRepo = cardRepo;
        this.transactionRepo = transactionRepo;
        this.broker = broker;
    }

    /**
     * Validate card + create a transaction + publish PAYMENT_AUTHORIZED /
     * PAYMENT_DENIED
     */
    public void authorizePayment(int userId, int cardId, double amount) {

        PaymentCard card = cardRepo.findById(cardId);
        if (card == null) {
            broker.publish(EventType.PAYMENT_DENIED, "Card not found");
            return;
        }

        // Policy
        if (amount > 2000) {
            broker.publish(EventType.PAYMENT_DENIED, "Amount exceeds limit");
            return;
        }

        PaymentTransaction txn = new PaymentTransaction(
                0,
                userId, // id generated later
                0, // orderId unknown now
                amount,
                System.currentTimeMillis(),
                card.getCardType(), // paymentMethod
                PaymentTransaction.Status.AUTHORIZED);

        transactionRepo.save(txn);
        broker.publish(EventType.PAYMENT_AUTHORIZED, txn);
    }
}
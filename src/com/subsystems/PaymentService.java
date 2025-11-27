package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;

import java.util.concurrent.CompletableFuture;
import com.broker.Listener;

// handle PAYMENT_AUTHORIZATION_REQUESTED
// Use AuthenticationService to simulate payment processing
// On success publish PAYMENT_AUTHORIZED, on failure publish PAYMENT_DENIED

public class PaymentService implements Subsystems {
    private AsyncMessageBroker broker;
    private Listener handlePaymentAuth = this::handlePaymentAuth;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.PAYMENT_AUTHORIZATION_REQUESTED, handlePaymentAuth);
    }

    @Override
    public void start() {

    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.PAYMENT_AUTHORIZATION_REQUESTED, this::handlePaymentAuth);
    }

    private CompletableFuture<Void> handlePaymentAuth(Message message) {
        return CompletableFuture
                .runAsync(() -> {
                    // Simulate async payment processing (network call to payment gateway)
                    System.out.println("[PaymentService] Processing payment authorization...");
                    try {
                        Thread.sleep(1000); // Simulate network delay
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    // Simulate payment success/failure (90% success rate)
                    if (Math.random() > 0.1) {
                        broker.publish(EventType.PAYMENT_AUTHORIZED, message.getPayload());
                        System.out.println("[Payment Service] Payment authorized");
                    } else {
                        broker.publish(EventType.PAYMENT_DENIED, message.getPayload());
                        System.out.println("[Payment Service] Payment denied");
                        ;
                    }
                });
    }
}

package com.subsystems;

import com.broker.*;
import java.util.concurrent.CompletableFuture;

public class PaymentManagement implements Subsystems {

    private AsyncMessageBroker broker;

    private final Listener handlePaymentAuth = this::processAuthorization;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.PAYMENT_AUTHORIZATION_REQUESTED, handlePaymentAuth);
        System.out.println("[PaymentService] Initialized");
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.PAYMENT_AUTHORIZATION_REQUESTED, handlePaymentAuth);
        System.out.println("[PaymentService] Shutdown complete");
    }

    private CompletableFuture<Void> processAuthorization(Message message) {

        return CompletableFuture.runAsync(() -> {

            System.out.println("[PaymentService] Processing payment authorization...");

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            boolean success = Math.random() > 0.1;

            Object payload = message.getPayload();

            if (success) {
                broker.publish(EventType.PAYMENT_AUTHORIZED, payload);
                System.out.println("[PaymentService] Payment authorized.");
            } else {
                broker.publish(EventType.PAYMENT_DENIED, payload);
                System.out.println("[PaymentService] Payment denied.");
            }
        });
    }
}
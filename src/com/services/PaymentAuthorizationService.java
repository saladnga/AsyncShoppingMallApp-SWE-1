package com.services;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.common.dto.payment.PaymentAuthorizeRequest;

import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PaymentAuthorizationService
 * -------------------------------------------------------------
 * Implements the “Payment Authorization” external service in the
 * PaymentProcess diagram.
 *
 * Responsibilities:
 * 1. Receive PAYMENT_AUTHORIZATION_REQUESTED
 * 2. Validate & simulate authorization
 * 3. Publish PAYMENT_AUTHORIZED or PAYMENT_DENIED
 *
 * NO RECEIPT EVENTS HERE — that belongs to EmailService.
 */

public class PaymentAuthorizationService {

    private static final Logger LOGGER = Logger.getLogger(PaymentAuthorizationService.class.getName());

    private final AsyncMessageBroker broker;
    private final ExecutorService authExecutor = Executors.newFixedThreadPool(4);
    private final Random random = new Random();

    public PaymentAuthorizationService(AsyncMessageBroker broker) {
        this.broker = broker;
        registerListener();
    }

    private void registerListener() {
        broker.registerListener(EventType.PAYMENT_AUTHORIZATION_REQUESTED, onAuthorizationRequest());
        LOGGER.info("[PaymentAuthorizationService] Listening for PAYMENT_AUTHORIZATION_REQUESTED");
    }

    private Listener onAuthorizationRequest() {
        return message -> {
            PaymentAuthorizeRequest req = (PaymentAuthorizeRequest) message.getPayload();
            return authorizeAsync(req);
        };
    }

    public CompletableFuture<Void> authorizeAsync(PaymentAuthorizeRequest req) {
        return CompletableFuture.runAsync(() -> {

            LOGGER.info("[PaymentAuthorization] Processing authorization for user "
                    + req.getUserId() + ", amount $" + req.getAmount());

            simulateBankDelay();

            boolean approved = simulateDecision(req);

            if (approved) {
                LOGGER.info("[PaymentAuthorization] APPROVED");
                broker.publish(EventType.PAYMENT_AUTHORIZED, req);
            } else {
                LOGGER.warning("[PaymentAuthorization] DENIED");
                broker.publish(EventType.PAYMENT_DENIED, req);
            }

        }, authExecutor).exceptionally(ex -> {

            LOGGER.log(Level.SEVERE, "[PaymentAuthorization] Error: ", ex);

            // Error → treat as denial
            broker.publish(EventType.PAYMENT_DENIED, req);
            return null;
        });
    }

    private void simulateBankDelay() {
        try {
            Thread.sleep(700 + random.nextInt(600));
        } catch (InterruptedException ignored) {
        }
    }

    private boolean simulateDecision(PaymentAuthorizeRequest req) {

        if (req.getAmount() <= 0)
            return false;

        if (req.getExpiryDate() == null || req.getExpiryDate().length() < 4)
            return false;

        // 80% approved
        return random.nextDouble() < 0.80;
    }

    public void shutdown() {
        LOGGER.info("[PaymentAuthorizationService] Shutting down...");
        authExecutor.shutdown();
    }
}
package com.services;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.entities.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * EmailService
 * ---------------------------------------------------------
 * - Supports normal email sending (used by SendReceiptManager)
 * - Supports async receipt sending (used by Payment flow)
 * - Cleaned and aligned with your current project structure
 */

public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private final AsyncMessageBroker broker;

    private final ExecutorService emailExecutor = Executors.newFixedThreadPool(3);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public EmailService(AsyncMessageBroker broker) {
        this.broker = broker;
    }

    // ======================================================
    // SIMPLE EMAIL SUPPORT (used by SendReceiptManager)
    // ======================================================
    public void sendEmail(int userId, String subject, String body) {
        LOGGER.info("[EmailService] Sending email to user " + userId);
        LOGGER.info("Subject: " + subject);
        LOGGER.info("Body:\n" + body);
    }

    // ======================================================
    // ASYNC RECEIPT SENDING (used by Payment flow)
    // ======================================================
    public CompletableFuture<Void> sendReceiptAsync(PaymentTransaction tx) {

        return CompletableFuture.runAsync(() -> {

            try {
                LOGGER.info("[EmailService] Preparing receipt...");

                String receipt = buildReceipt(tx);
                simulateDelay();

                LOGGER.info("[EmailService] Sending receipt to USER " + tx.getUserId());
                LOGGER.info(receipt);

                // Publish event (PAYMENT_RECEIPT_SENT)
                broker.publish(EventType.EMAIL_RECEIPT_REQUESTED, tx);

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "[EmailService] FAILED to send receipt", ex);

                // Publish failure
                broker.publish(EventType.ORDER_PAYMENT_FAILED, tx);
            }

        }, emailExecutor);
    }

    // ======================================================
    // RECEIPT GENERATOR (does not use removed fields)
    // ======================================================
    private String buildReceipt(PaymentTransaction tx) {

        return """
                =======================================
                       OFFICIAL PAYMENT RECEIPT
                =======================================
                Transaction ID : %d
                User ID        : %d
                Amount Paid    : $%.2f
                Method         : %s
                Status         : %s
                Processed At   : %s
                =======================================
                Thank you for your purchase!
                =======================================
                """
                .formatted(
                        tx.getId(),
                        tx.getUserId(),
                        tx.getAmount(),
                        tx.getPaymentMethod(),
                        tx.getStatus(),
                        LocalDateTime.now());
    }

    // ======================================================
    // DELAY SIMULATION
    // ======================================================
    private void simulateDelay() {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            scheduler.schedule(latch::countDown, 1, TimeUnit.SECONDS);
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }

    // ======================================================
    // SHUTDOWN HANDLERS
    // ======================================================
    public void shutdown() {
        emailExecutor.shutdown();
        scheduler.shutdown();
    }
}
package com.services;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.entities.Order;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RefundService {

    private static final Logger LOGGER = Logger.getLogger(RefundService.class.getName());

    private final AsyncMessageBroker broker;
    private final ExecutorService refundExecutor = Executors.newFixedThreadPool(3);

    public RefundService(AsyncMessageBroker broker) {
        this.broker = broker;
        registerListeners();
    }

    private void registerListeners() {
        broker.registerListener(EventType.REFUND_PROCESS_REQUESTED, handleRefund());
        LOGGER.info("[RefundService] Listener registered for REFUND_PROCESS_REQUESTED");
    }

    private Listener handleRefund() {
        return msg -> processRefundAsync(msg.getPayload());
    }

    public CompletableFuture<Void> processRefundAsync(Object payload) {

        return CompletableFuture.runAsync(() -> {

            LOGGER.info("[RefundService] Starting refund process...");

            try {
                if (!(payload instanceof Order order)) {
                    throw new IllegalArgumentException("Refund payload must be Order");
                }

                simulateRefundDelay();

                boolean success = Math.random() > 0.15;

                if (success) {
                    LOGGER.info("[RefundService] Refund completed for order " + order.getId());

                    broker.publish(EventType.REFUND_SUCCESS, order);

                } else {
                    LOGGER.warning("[RefundService] Refund FAILED for order " + order.getId());

                    broker.publish(EventType.REFUND_FAILED, order);
                }

            } catch (Exception ex) {

                LOGGER.log(Level.SEVERE, "[RefundService] Exception during refund!", ex);

                broker.publish(EventType.REFUND_FAILED, payload);
            }

        }, refundExecutor);
    }

    private void simulateRefundDelay() {
        try {
            Thread.sleep(800);
        } catch (InterruptedException ignored) {
        }
    }

    public void shutdown() {
        LOGGER.info("[RefundService] Shutting down...");
        refundExecutor.shutdown();
    }
}
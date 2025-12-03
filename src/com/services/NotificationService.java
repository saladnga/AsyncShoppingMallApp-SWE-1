package com.services;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationService {

    private static final Logger LOGGER = Logger.getLogger(NotificationService.class.getName());

    private final AsyncMessageBroker broker;

    private final ExecutorService notifyExecutor = Executors.newFixedThreadPool(4);

    private final Listener notificationListener = msg -> sendNotificationAsync(msg.getEventType(), msg.getPayload());

    public NotificationService(AsyncMessageBroker broker) {
        this.broker = broker;
        registerListeners();
    }

    private void registerListeners() {

        EventType[] eventList = {
                EventType.ORDER_CONFIRMED,
                EventType.ORDER_CANCEL_SUCCESS,
                EventType.PAYMENT_AUTHORIZED,
                EventType.PAYMENT_DENIED,
                EventType.MESSAGE_SENT_CONFIRMATION,
                EventType.WISHLIST_ADD_SUCCESS,
                EventType.WISHLIST_REMOVE_SUCCESS,
                EventType.REPORT_GENERATION_COMPLETE,
                EventType.SHIPPING_STATUS_UPDATED
        };

        for (EventType e : eventList) {
            broker.registerListener(e, notificationListener);
        }

        LOGGER.info("[NotificationService] Listeners registered: " + eventList.length);
    }

    private String buildNotification(EventType type, Object payload) {

        return switch (type) {

            case ORDER_CONFIRMED -> "Your order has been confirmed.";
            case ORDER_CANCEL_SUCCESS -> "Your order was cancelled.";
            case PAYMENT_AUTHORIZED -> "Your payment was authorized.";
            case PAYMENT_DENIED -> "Your payment was denied.";
            case MESSAGE_SENT_CONFIRMATION -> "You received a new message.";
            case WISHLIST_ADD_SUCCESS -> "Item added to your wishlist.";
            case WISHLIST_REMOVE_SUCCESS -> "Item removed from wishlist.";
            case REPORT_GENERATION_COMPLETE -> "A new report is ready.";
            case SHIPPING_STATUS_UPDATED -> "Your shipping status was updated.";

            default -> "You have a new notification.";
        };
    }

    public CompletableFuture<Void> sendNotificationAsync(EventType type, Object payload) {

        return CompletableFuture.runAsync(() -> {

            try {
                String content = buildNotification(type, payload);

                Thread.sleep(200); // simulate delay

                LOGGER.info("""
                        [NotificationService] Notification Sent:
                        %s
                        Time: %s
                        """.formatted(content, LocalDateTime.now()));

                broker.publish(
                        EventType.NOTIFICATION_SENT,
                        content);

            } catch (Exception ex) {

                LOGGER.log(Level.SEVERE,
                        "[NotificationService] FAILED to send notification",
                        ex);

                broker.publish(
                        EventType.NOTIFICATION_FAILED,
                        payload);
            }

        }, notifyExecutor);
    }

    public void shutdown() {
        notifyExecutor.shutdown();
        LOGGER.info("[NotificationService] Executor shutdown.");
    }
}
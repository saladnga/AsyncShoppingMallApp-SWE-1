package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;
import java.util.concurrent.CompletableFuture;
import com.entities.UserMessage;
import com.broker.Listener;

// Persist UserMessage, notify staff, publish MESSAGE_SEND_CONFIRMATION
//! handleMessageSend:
//  Cast payload to UserMessage, save to DB, publish STAFF_NOTIFICATION_NEW_MESSAGE to inform staff listeners

public class Messaging implements Subsystems {
    private AsyncMessageBroker broker;
    private Listener handleMessageSend = this::handleMessageSend;
    private Listener handleMessageReply = this::handleMessageReply;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.MESSAGE_SEND_REQUESTED, handleMessageSend);
        broker.registerListener(EventType.MESSAGE_REPLY_REQUESTED, handleMessageReply);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.MESSAGE_SEND_REQUESTED, handleMessageSend);
        broker.unregisterListener(EventType.MESSAGE_REPLY_REQUESTED, handleMessageReply);
    }

    private CompletableFuture<Void> handleMessageSend(Message message) {
        // Cast payload to com.entities.UserMessage and persist to database

        return CompletableFuture.runAsync(() -> {
            System.out.println("[Messaging] Processing message...");
            broker.publish(EventType.MESSAGE_SENT_CONFIRMATION, message.getPayload());
            broker.publish(EventType.STAFF_NOTIFIED_NEW_MESSAGE, message.getPayload());
        });
    }

    private CompletableFuture<Void> handleMessageReply(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Messaging] Replying message...");

            broker.publish(EventType.MESSAGE_SENT_CONFIRMATION, message.getPayload());
            broker.publish(EventType.CUSTOMER_NOTIFIED_NEW_REPLY, message.getPayload());
        });
    }
}

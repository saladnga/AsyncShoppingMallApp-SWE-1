package com.managers.message;

import com.broker.*;
import com.common.dto.message.MessageHistoryRequest;
import com.common.dto.message.ConversationLoadRequest;
import com.entities.UserMessage;
import com.repository.MessageRepository;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoadConversationManager implements Listener {

    private final AsyncMessageBroker broker;
    private final MessageRepository repo;

    public LoadConversationManager(AsyncMessageBroker broker, MessageRepository repo) {
        this.broker = broker;
        this.repo = repo;
        broker.registerListener(EventType.MESSAGE_HISTORY_REQUESTED, this);
        broker.registerListener(EventType.CONVERSATION_LOAD_REQUESTED, this);
    }

    @Override
    public CompletableFuture<Void> onMessage(Message message) {
        return CompletableFuture.runAsync(() -> {

            if (message.getPayload() instanceof ConversationLoadRequest req) {
                int viewerId = req.getUserId();
                int customerId;

                // If otherUserId == -1, this is the customer viewing their own conversation(s)
                // Otherwise, staff is viewing conversation with a specific customer
                if (req.getOtherUserId() == -1) {
                    customerId = viewerId;
                } else {
                    customerId = req.getOtherUserId();
                }

                // Load full conversation for this customer (all messages between customer and staff)
                List<UserMessage> messages = repo.getConversationMessages(customerId, viewerId);

                // Mark messages as read for this viewer
                repo.markMessagesAsRead(customerId, viewerId);

                broker.publish(EventType.CONVERSATION_MESSAGES_RETURNED, messages);
                broker.publish(EventType.MESSAGE_MARKED_AS_READ, req);

                System.out.println("[LoadConversationManager] Loaded " + messages.size()
                        + " messages for customer " + customerId + " (viewer=" + viewerId + ")");

            } else if (message.getPayload() instanceof MessageHistoryRequest req) {
                // Legacy support for MESSAGE_HISTORY_REQUESTED
                var conversation = repo.getConversation(req.getUserId(), req.getStaffId());
                repo.markRead(req.getUserId(), req.getStaffId());
                broker.publish(EventType.MESSAGE_HISTORY_RETURNED, conversation);
            }
        });
    }
}
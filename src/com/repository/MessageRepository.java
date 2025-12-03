package com.repository;

import com.entities.UserMessage;
import com.entities.Conversation;
import java.util.List;

public abstract class MessageRepository {

        // ===== CREATE =====
        public abstract UserMessage save(UserMessage msg);

        // ===== READ: full conversation =====
        public abstract List<UserMessage> getConversation(int userId, int staffId);

        // ===== READ: unread messages =====
        public abstract List<UserMessage> getUnreadMessages(int userId, int staffId);

        // ===== UPDATE: mark as read =====
        public abstract void markRead(int userId, int staffId);

        // ===== READ: distinct partners =====
        public abstract List<Integer> getConversationPartners(int staffId);

        // ===== READ: message history for user =====
        public abstract List<UserMessage> getMessageHistory(int userId);

        // ===== READ: conversations for user (with conversation metadata) =====
        public abstract List<Conversation> getConversationsForUser(int userId);

        // ===== MISSING METHODS THAT MANAGERS NEED =====

        // Used by UnreadMessageManager
        public abstract List<UserMessage> getUnreadMessagesForStaff(int staffId);

        // Used by LoadConversationManager
        public abstract List<UserMessage> getConversationMessages(int userId, int otherUserId);

        public abstract void markMessagesAsRead(int userId, int otherUserId);

        // Used by SendMessageManager
        public abstract void markCustomerMessagesAsReadByStaff(int customerId, int staffId);
}
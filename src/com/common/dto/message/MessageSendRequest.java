package com.common.dto.message;

public final class MessageSendRequest {

    private final int senderId;
    private final int recipientId;
    private final String subject;
    private final String content;

    public MessageSendRequest(int senderId, int recipientId, String subject, String content) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.subject = subject;
        this.content = content;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        String preview = (content != null)
                ? content.substring(0, Math.min(30, content.length())) + "..."
                : null;

        return "MessageSendRequest{" +
                "senderId=" + senderId +
                ", recipientId=" + recipientId +
                ", subject='" + subject + '\'' +
                ", contentPreview='" + preview + '\'' +
                '}';
    }
}
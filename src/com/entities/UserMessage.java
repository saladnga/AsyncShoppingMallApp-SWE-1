package com.entities;

/**
 * Represents a message sent between users in the system.
 *
 * Notes:
 * - senderId references the user who sent the message.
 * - recipientId references the user who receives the message (often staff).
 * - MessageStatus tracks whether the message has been read or not.
 * - timeStamp records when the message was created (epoch milliseconds).
 */

public class UserMessage {
    public enum MessageStatus {
        UNREAD,
        READ
    }

    private int id;
    private int senderId; // FK to users(id)
    private int recipientId; // FK to users(id) - staff
    private String subject;
    private String content;
    private MessageStatus status;
    private long timeStamp;

    public UserMessage() {

    }

    public UserMessage(int id, int senderId, int recipientId, String subject, String content, MessageStatus status,
            long timeStamp) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.timeStamp = timeStamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void markAsRead() {
        this.status = MessageStatus.READ;
    }

    public void markAsUnread() {
        this.status = MessageStatus.UNREAD;
    }
}

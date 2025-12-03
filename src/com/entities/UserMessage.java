package com.entities;

public class UserMessage {

    public enum MessageStatus {
        UNREAD, READ
    }

    private int id;
    private int senderId;
    private int recipientId;
    private String subject;
    private String content;
    private MessageStatus status;
    private long timestamp;

    /** Auto timestamp for new message */
    public UserMessage() {
        this.status = MessageStatus.UNREAD;
        this.timestamp = System.currentTimeMillis();
    }

    public UserMessage(int id, int senderId, int recipientId,
            String subject, String content,
            MessageStatus status, long timestamp) {

        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.subject = subject;
        this.content = content;
        this.status = status;
        this.timestamp = timestamp;
    }

    /* getters & setters */
    public int getId() {
        return id;
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

    public MessageStatus getStatus() {
        return status;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTimeStamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setRecipientId(int recipientId) {
        this.recipientId = recipientId;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /** Business logic */
    public void markAsRead() {
        this.status = MessageStatus.READ;
    }

    public void markAsUnread() {
        this.status = MessageStatus.UNREAD;
    }
}
package com.common.dto.message;

public final class MessageReplyRequest {

    private final int staffId;
    private final int userId;
    private final String content;

    public MessageReplyRequest(int staffId, int userId, String content) {
        this.staffId = staffId;
        this.userId = userId;
        this.content = content;
    }

    public int getStaffId() {
        return staffId;
    }

    public int getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        String preview = (content != null)
                ? content.substring(0, Math.min(30, content.length())) + "..."
                : null;

        return "MessageReplyRequest{" +
                "staffId=" + staffId +
                ", userId=" + userId +
                ", contentPreview='" + preview + '\'' +
                '}';
    }
}
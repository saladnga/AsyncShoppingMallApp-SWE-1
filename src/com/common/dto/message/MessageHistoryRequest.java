package com.common.dto.message;

public final class MessageHistoryRequest {
    private final int userId;
    private final int staffId;

    public MessageHistoryRequest(int userId, int staffId) {
        this.userId = userId;
        this.staffId = staffId;
    }

    public int getUserId() {
        return userId;
    }

    public int getStaffId() {
        return staffId;
    }

    @Override
    public String toString() {
        return "MessageHistoryRequest{" +
                "userId=" + userId +
                ", staffId=" + staffId +
                '}';
    }
}
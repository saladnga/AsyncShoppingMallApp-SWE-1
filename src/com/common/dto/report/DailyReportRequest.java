package com.common.dto.report;

public class DailyReportRequest {

    private final int requesterId;
    private final long date;

    public DailyReportRequest(int requesterId) {
        this.requesterId = requesterId;
        this.date = System.currentTimeMillis();
    }

    public DailyReportRequest(int requesterId, long date) {
        this.requesterId = requesterId;
        this.date = date;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public long getDate() {
        return date;
    }

    @Override
    public String toString() {
        return "DailyReportRequest{requesterId=" + requesterId + ", date=" + date + "}";
    }
}
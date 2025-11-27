package com.entities;

/**
 * Represents a generated report in the system.
 *
 * Notes:
 * - reportData stores a JSON summary or aggregated data.
 * - dataStart and dateGenerated are timestamps used for data ranges and
 * tracking.
 */

public class Report {
    public enum ReportType {
        DAILY,
        MONTHLY
    }

    private int id;
    private ReportType reportType;
    private long dataStart;
    private long dateGenerated;
    private String reportData;

    public Report() {

    }

    public Report(int id, ReportType reportType, long dataStart, long dateGenerated, String reportData) {
        this.id = id;
        this.reportType = reportType;
        this.dataStart = dataStart;
        this.dateGenerated = dateGenerated;
        this.reportData = reportData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public long getDataStart() {
        return dataStart;
    }

    public void setDataStart(long dataStart) {
        this.dataStart = dataStart;
    }

    public long getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(long dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    public String getReportData() {
        return reportData;
    }

    public void setReportData(String reportData) {
        this.reportData = reportData;
    }

    public String getSummary() {
        if (reportData == null)
            return "";
        return reportData.length() <= 100 ? reportData : reportData.substring(0, 100) + "...";
    }
}

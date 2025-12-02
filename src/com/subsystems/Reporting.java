package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Listener;
import com.broker.Message;
import com.entities.Report;
import com.entities.User;
import com.managers.report.ReportManager;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Reporting Subsystem
 * ------------------------------------------------------------
 * - Generates DAILY & MONTHLY reports when triggered by TimeActor
 * - Only CEO is allowed to view reports
 * - generateReport() returns a FORM (structure only), not sample data
 */
public class Reporting implements Subsystems {
    private final ReportManager reportManager;

    public Reporting(ReportManager reportManager) {
        this.reportManager = reportManager;
    }

    private static final Logger LOGGER = Logger.getLogger(Reporting.class.getName());

    private AsyncMessageBroker broker;

    private final Listener handleDailyReport = this::onDailyReportTriggered;
    private final Listener handleMonthlyReport = this::onMonthlyReportTriggered;
    private final Listener handleReportView = this::onReportViewRequested;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;

        broker.registerListener(EventType.TIMER_TRIGGER_DAILY_REPORT, handleDailyReport);
        broker.registerListener(EventType.TIMER_TRIGGER_MONTHLY_REPORT, handleMonthlyReport);
        broker.registerListener(EventType.REPORT_VIEW_REQUESTED, handleReportView);

        LOGGER.info("[Reporting] Subsystem initialized.");
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.TIMER_TRIGGER_DAILY_REPORT, handleDailyReport);
        broker.unregisterListener(EventType.TIMER_TRIGGER_MONTHLY_REPORT, handleMonthlyReport);
        broker.unregisterListener(EventType.REPORT_VIEW_REQUESTED, handleReportView);

        LOGGER.info("[Reporting] Subsystem shutdown.");
    }

    /** DAILY REPORT HANDLER */
    private CompletableFuture<Void> onDailyReportTriggered(Message message) {
        return CompletableFuture.runAsync(() -> {

            LOGGER.info("[Reporting] Generating DAILY report...");

            Report report = generateReport(Report.ReportType.DAILY);

            broker.publish(EventType.REPORT_GENERATION_COMPLETE, report);

        });
    }

    /** MONTHLY REPORT HANDLER */
    private CompletableFuture<Void> onMonthlyReportTriggered(Message message) {
        return CompletableFuture.runAsync(() -> {

            LOGGER.info("[Reporting] Generating MONTHLY report...");

            Report report = generateReport(Report.ReportType.MONTHLY);

            broker.publish(EventType.REPORT_GENERATION_COMPLETE, report);

        });
    }

    /** CEO REPORT VIEW HANDLER */
    private CompletableFuture<Void> onReportViewRequested(Message message) {
        return CompletableFuture.runAsync(() -> {

            Object payload = message.getPayload();
            if (!(payload instanceof User viewer)) {
                LOGGER.warning("[Reporting] Invalid payload for report view.");
                return;
            }

            // Authorization check
            if (viewer.getRole() != User.Role.CEO) {
                broker.publish(EventType.REPORT_DETAILS_RETURNED,
                        "ACCESS DENIED: Only CEO can view reports.");
                return;
            }

            LOGGER.info("[Reporting] CEO requesting report view.");

            // Only return form, not sample data
            String reportStructure = """
                    --- REPORT STRUCTURE ---
                    TYPE: DAILY or MONTHLY
                    DATE RANGE: dynamic
                    FIELDS:
                      - totalSales
                      - totalOrders
                      - items: [
                           { productId, productName, quantitySold }
                        ]
                      - generatedAt
                    -------------------------
                    """;

            broker.publish(EventType.REPORT_DETAILS_RETURNED, reportStructure);
        });
    }

    private Report generateReport(Report.ReportType type) {
        Report report;
        switch (type) {
            case DAILY -> {
                report = reportManager.generateDailyReport();
            }
            case MONTHLY -> {
                report = reportManager.generateMonthlyReport();
            }
            default -> throw new IllegalArgumentException("Unknown report type: " + type);
        }
        
        // TC28: Empty Report - check if report has no sales data
        if (report != null && report.getSummary() != null) {
            String summary = report.getSummary();
            // Check if totalSales is 0.0 or totalOrders is 0
            if (summary.contains("\"totalSales\": 0.0") || summary.contains("\"totalOrders\": 0")) {
                LOGGER.info("[Reporting] Generated empty report (no sales data found)");
            }
        }
        
        return report;
    }
}
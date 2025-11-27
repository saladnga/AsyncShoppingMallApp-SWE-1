package com.subsystems;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.broker.Message;
import java.util.concurrent.CompletableFuture;
import com.entities.Report;
import com.broker.Listener;

// Handle timer events to generate and persist reports, publish REPORT_GENERATION_COMPLETE
// On TIMER_TRIGGER_DAILY_REPORT, query database for sales during day range then create Report object, save to DB
// CEO is allowed only to query reports -> enforce authorization when serving REPORT_VIEW_REQUESTED

public class Reporting implements Subsystems {
    private AsyncMessageBroker broker;
    private Listener handleDailyReport = this::handleDailyReport;
    private Listener handleMonthlyReport = this::handleMonthlyReport;
    private Listener handleReportView = this::handleReportView;

    @Override
    public void init(AsyncMessageBroker broker) {
        this.broker = broker;
        broker.registerListener(EventType.TIMER_TRIGGER_DAILY_REPORT, handleDailyReport);
        broker.registerListener(EventType.TIMER_TRIGGER_MONTHLY_REPORT, handleMonthlyReport);
        broker.registerListener(EventType.REPORT_VIEW_REQUESTED, handleReportView);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        broker.unregisterListener(EventType.TIMER_TRIGGER_DAILY_REPORT, handleDailyReport);
        broker.unregisterListener(EventType.TIMER_TRIGGER_MONTHLY_REPORT, handleMonthlyReport);
        broker.unregisterListener(EventType.REPORT_VIEW_REQUESTED, handleReportView);
    }

    private CompletableFuture<Void> handleDailyReport(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Reporting] Generating daily report...");
            broker.publish(EventType.REPORT_GENERATION_COMPLETE, "Daily report generated");
        });
    }

    private CompletableFuture<Void> handleMonthlyReport(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Reporting] Generating monthly report...");
            broker.publish(EventType.REPORT_GENERATION_COMPLETE, "Monthly report generated");
        });
    }

    private CompletableFuture<Void> handleReportView(Message message) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("[Reporting] Generating sales report...");

            try {
                Thread.sleep(1000); // Simulate report generation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            String reportData = "Sales Report - Total Sales: $10,234.56, Orders: 42, Top Item: Laptop";
            broker.publish(EventType.REPORT_DETAILS_RETURNED, reportData);

            System.out.println("[Reporting] Sales report generated");
        });
    }
}

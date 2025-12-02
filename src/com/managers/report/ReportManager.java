package com.managers.report;

import com.entities.Report;
import com.repository.ReportRepository;
import com.repository.OrderRepository;
import com.repository.ItemRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public class ReportManager {
    private final ReportRepository reportRepo;
    private final OrderRepository orderRepo; // Can be null for now
    private final ItemRepository itemRepo;

    public ReportManager(ReportRepository reportRepo, OrderRepository orderRepo, ItemRepository itemRepo) {
        this.reportRepo = reportRepo;
        this.orderRepo = orderRepo; // Will be null until OrderRepository is implemented
        this.itemRepo = itemRepo;
    }

    public Report generateDailyReport() {
        LocalDate today = LocalDate.now();
        long startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endOfDay = today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return generateReportForPeriod(Report.ReportType.DAILY, startOfDay, endOfDay);
    }

    public Report generateMonthlyReport() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        long startTime = startOfMonth.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTime = startOfNextMonth.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        return generateReportForPeriod(Report.ReportType.MONTHLY, startTime, endTime);
    }

    /**
     * Generate report for specific time period
     */
    private Report generateReportForPeriod(Report.ReportType type, long startTime, long endTime) {
        // Get actual data from repositories
        double totalSales = calculateTotalSales(startTime, endTime);
        int totalOrders = calculateTotalOrders(startTime, endTime);
        List<String> topItems = getTopSellingItems(startTime, endTime);
        int totalItems = getTotalItemsInInventory();

        String reportData = String.format(
                """
                        {
                          "reportType": "%s",
                          "period": {
                            "start": %d,
                            "end": %d,
                            "startDate": "%s",
                            "endDate": "%s"
                          },
                          "generatedAt": %d,
                          "generatedDate": "%s",
                          "summary": {
                            "totalSales": %.2f,
                            "totalOrders": %d,
                            "averageOrderValue": %.2f,
                            "totalItemsInInventory": %d,
                            "reportPeriod": "%s"
                          },
                          "topSellingItems": [
                            %s
                          ],
                          "notes": "This report shows %s sales data from %s to %s. Orders and sales data are simulated for demonstration."
                        }
                        """,
                type.name(),
                startTime,
                endTime,
                new java.util.Date(startTime).toString(),
                new java.util.Date(endTime).toString(),
                Instant.now().toEpochMilli(),
                new java.util.Date().toString(),
                totalSales,
                totalOrders,
                totalOrders > 0 ? totalSales / totalOrders : 0.0,
                totalItems,
                type.name().toLowerCase(),
                String.join(",\\n    ", topItems),
                type.name().toLowerCase(),
                new java.util.Date(startTime).toString(),
                new java.util.Date(endTime).toString());

        Report report = new Report(
                0,
                type,
                startTime,
                Instant.now().toEpochMilli(),
                reportData);

        reportRepo.insert(report);
        return report;
    }

    /**
     * Calculate total sales for period (simulated for now)
     * TC28: Empty Report - return 0.0 when no sales exist
     */
    private double calculateTotalSales(long startTime, long endTime) {
        // TC28: If orderRepo is available, query actual sales
        if (orderRepo != null) {
            // TODO: Implement actual query when OrderRepository has findByDateRange method
            // For now, return 0.0 to simulate empty report scenario
            // return orderRepo.findByDateRange(startTime, endTime).stream()
            //     .mapToDouble(Order::getTotalAmount).sum();
        }
        
        // For now, return simulated data based on items in inventory
        if (itemRepo != null) {
            List<com.entities.Item> items = itemRepo.findAll();
            if (items.isEmpty()) {
                // TC28: Empty Report - return 0.0 when no items
                return 0.0;
            }
            return items.stream().mapToDouble(item -> item.getPrice()).sum() * 0.1; // Simulate 10% of inventory value as sales
        }
        // TC28: Return 0.0 for empty report instead of simulated value
        return 0.0;
    }

    /**
     * Calculate total orders for period (simulated for now)
     * TC28: Empty Report - return 0 when no orders exist
     */
    private int calculateTotalOrders(long startTime, long endTime) {
        // TC28: If orderRepo is available, query actual orders
        if (orderRepo != null) {
            // TODO: Implement actual query when OrderRepository has findByDateRange method
            // For now, return 0 to simulate empty report scenario
            // return orderRepo.findByDateRange(startTime, endTime).size();
        }
        
        // TC28: Return 0 for empty report instead of random value
        return 0;
    }

    /**
     * Get top selling items (simulated for now)
     */
    private List<String> getTopSellingItems(long startTime, long endTime) {
        List<String> topItems = new java.util.ArrayList<>();

        if (itemRepo != null) {
            List<com.entities.Item> items = itemRepo.findAll();
            int limit = Math.min(5, items.size()); // Top 5 items or all if less than 5

            for (int i = 0; i < limit; i++) {
                com.entities.Item item = items.get(i);
                int simulatedSales = (int) (Math.random() * 20) + 1;
                String itemJson = String.format(
                        "{ \"itemId\": %d, \"name\": \"%s\", \"price\": %.2f, \"unitsSold\": %d, \"revenue\": %.2f }",
                        item.getId(), item.getName(), item.getPrice(), simulatedSales,
                        item.getPrice() * simulatedSales);
                topItems.add(itemJson);
            }
        } else {
            // Default simulated items
            topItems.add(
                    "{ \"itemId\": 1, \"name\": \"Sample Item 1\", \"price\": 29.99, \"unitsSold\": 15, \"revenue\": 449.85 }");
            topItems.add(
                    "{ \"itemId\": 2, \"name\": \"Sample Item 2\", \"price\": 19.99, \"unitsSold\": 12, \"revenue\": 239.88 }");
            topItems.add(
                    "{ \"itemId\": 3, \"name\": \"Sample Item 3\", \"price\": 39.99, \"unitsSold\": 8, \"revenue\": 319.92 }");
        }

        return topItems;
    }

    /**
     * Get total items in inventory
     */
    private int getTotalItemsInInventory() {
        if (itemRepo != null) {
            List<com.entities.Item> items = itemRepo.findAll();
            return items.stream().mapToInt(com.entities.Item::getStockQuantity).sum();
        }
        return 150; // Default simulated value
    }

    /**
     * Get all reports
     */
    public List<Report> getAllReports() {
        return reportRepo.findAll();
    }

    /**
     * Get reports by type
     */
    public List<Report> getReportsByType(Report.ReportType type) {
        return reportRepo.findByType(type);
    }

    /**
     * Get latest report of specific type
     */
    public Report getLatestReport(Report.ReportType type) {
        return reportRepo.findLatestByType(type);
    }

    /**
     * Format report for display
     */
    public String formatReportForDisplay(Report report) {
        if (report == null) {
            return "No report found.";
        }

        return String.format("""
                === %s REPORT ===
                Generated: %s
                Period Start: %s

                %s

                ==================
                """,
                report.getReportType(),
                new java.util.Date(report.getDateGenerated()),
                new java.util.Date(report.getDataStart()),
                report.getSummary());
    }
}

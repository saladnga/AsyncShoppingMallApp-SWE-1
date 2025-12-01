package com.ui;

import com.Main;
import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.common.dto.report.DailyReportRequest;
import com.common.dto.report.MonthlyReportRequest;
import com.entities.User;

import java.util.List;
import java.util.Scanner;

public class CeoUI {

    public static void showMenu(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.clear();

        UIHelper.box(
                UIHelper.color("CEO MENU", UIHelper.MAGENTA),
                List.of(
                        "1. View Sales Reports",
                        "2. Logout",
                        "Q. Quit"));

        System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
        String input = scanner.nextLine();

        switch (input) {
            case "1" -> viewSalesReport(scanner, broker);
            case "2" -> Main.currentUser = null;
            case "Q", "q" -> System.exit(0);
            default -> {
                System.out.println(UIHelper.RED + "Invalid option!" + UIHelper.RESET);
                UIHelper.pause();
            }
        }
    }


    // ============================================================
    // VIEW SALES REPORTS
    // ============================================================

    private static void viewSalesReport(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null || Main.currentUser.getRole() != User.Role.CEO) {
            System.out.println(UIHelper.RED + "Access denied. Only CEO can view reports" + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        UIHelper.box(UIHelper.color("VIEW SALES REPORTS", UIHelper.MAGENTA),
                List.of("1. Daily Report", "2. Monthly Report", "3. Back"));
        System.out.print(UIHelper.YELLOW + "Choose report type: " + UIHelper.RESET);

        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                DailyReportRequest request = new DailyReportRequest(Main.currentUser.getId());
                Object resp = BrokerUtils.requestOnce(broker, EventType.TIMER_TRIGGER_DAILY_REPORT, request,
                        EventType.REPORT_DETAILS_RETURNED, 5000);

                if (resp instanceof com.entities.Report report) {
                    System.out.println(UIHelper.CYAN + "--- DAILY REPORT ---" + UIHelper.RESET);
                    System.out.println("Report ID: " + report.getId());
                    System.out.println("Type: " + report.getReportType());
                    System.out.println("Generated: " + report.getDateGenerated());
                    System.out.println("--- Content ---\n" + report.getReportData());
                } else {
                    System.out.println(
                            UIHelper.YELLOW + "No daily report available or generation timed out." + UIHelper.RESET);
                }
                UIHelper.pause();
            }
            case "2" -> {
                MonthlyReportRequest request = new MonthlyReportRequest(Main.currentUser.getId());
                Object resp = BrokerUtils.requestOnce(broker, EventType.TIMER_TRIGGER_MONTHLY_REPORT, request,
                        EventType.REPORT_DETAILS_RETURNED, 5000);

                if (resp instanceof com.entities.Report report) {
                    System.out.println(UIHelper.CYAN + "--- MONTHLY REPORT ---" + UIHelper.RESET);
                    System.out.println("Report ID: " + report.getId());
                    System.out.println("Type: " + report.getReportType());
                    System.out.println("Generated: " + report.getDateGenerated());
                    System.out.println("--- Content ---\n" + report.getReportData());
                } else {
                    System.out.println(
                            UIHelper.YELLOW + "No monthly report available or generation timed out." + UIHelper.RESET);
                }
                UIHelper.pause();
            }
            case "3" -> {
                return;
            }
            default -> {
                System.out.println(UIHelper.RED + "Invalid choice" + UIHelper.RESET);
                UIHelper.pause();
            }
        }
    }
}
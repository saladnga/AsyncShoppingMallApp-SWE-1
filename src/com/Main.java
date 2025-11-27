package com;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.entities.User;
import com.services.TimeActor;
import com.common.Database;
import com.subsystems.*;
import java.util.Scanner;
import com.common.dto.*;

public class Main {
    private static User currentUser = null;
    private static Scanner scanner = new Scanner(System.in);
    private static AccountManagement account;
    private static ItemManagement item;
    private static Messaging message;
    private static OrderManagement order;
    private static PaymentService payment;
    private static Reporting report;
    private static WishlistManagement wishlist;
    private static AsyncMessageBroker broker;
    private static Database database;

    public static void main(String[] args) {
        System.out.println("[Main] Application Started");

        // Initialize Observer and Subsystems
        // Register subsystems with the broker so listeners are in place
        broker = new AsyncMessageBroker(1000, 8);
        broker.start();

        // Initialize DB (executes schema.sql). Minimal wiring only.
        database = new Database();
        database.connect("jdbc:sqlite:shopping_mall.db");

        initializeSubsystem(broker);

        // Start schedules timer event
        TimeActor timer = new TimeActor(broker);
        timer.start();

        // Input loop for menu
        while (true) {
            if (currentUser == null) {
                showUnregisteredMenu();
            } else {
                switch (currentUser.getRole()) {
                    case Customer:
                        showCustomerMenu();
                        break;
                    case Staff:
                        showStaffMenu();
                        break;
                    case CEO:
                        showCEOMenu();
                        break;
                }
            }

            String input = scanner.nextLine();
            if (input.equals("Q") || input.equals("q")) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }

        // Cleanup
        System.out.println("Exiting...");
        scanner.close();
        timer.stop();

        account.shutdown();
        item.shutdown();
        message.shutdown();
        order.shutdown();
        payment.shutdown();
        report.shutdown();
        wishlist.shutdown();
        // Close DB and broker
        if (database != null) {
            database.close();
        }

        broker.stop();
        System.exit(0);

    }

    private static void showUnregisteredMenu() {
        System.out.println("\n---Welcome to Shopping Mall App---");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Browse Items");
        System.out.println("Q. Quit");
        System.out.println("Select an option:");

        String input = scanner.nextLine();

        switch (input) {
            case "1":
                login(broker);
                break;
            case "2":
                register(broker);
                break;
            case "3":
                browse(broker);
                break;
            default:
                System.out.println("[Main App] Invalid selection");
        }
    }

    private static void showCustomerMenu() {
        System.out.println("\n---Customer Menu---");
        System.out.println("1. Browse Items");
        System.out.println("2. Search Items");
        System.out.println("3. View Wishlist");
        System.out.println("4. Purchase Item");
        System.out.println("5. View My Orders");
        System.out.println("6. Send Message to Staff");
        System.out.println("7. View Account");
        System.out.println("8. Log out");
        System.out.println("Select an option:");

        String input = scanner.nextLine();

        switch (input) {
            case "1":
                browse(broker);
                break;
            case "2":
                search(broker);
                break;
            case "3":
                viewWishlist(broker);
                break;
            case "4":
                purchase(broker);
                break;
            case "5":
                viewOrder(broker);
                break;
            case "6":
                sendMessageToStaff(broker);
                break;
            case "7":
                viewAccount(broker);
                break;
            case "8":
                logout(broker);
                break;
        }
    }

    private static void showStaffMenu() {
        System.out.println("\n---Staff Menu---");
        System.out.println("1. Refill Inventory");
        System.out.println("2. Upload New Item");
        System.out.println("3. Edit Item Info");
        System.out.println("4. Reply to Customer Messages");
        System.out.println("5. Log out");
        System.out.println("Select an option:");

        String input = scanner.nextLine();

        switch (input) {
            case "1":
                refill(broker);
                break;
            case "2":
                upload(broker);
                break;
            case "3":
                editItem(broker);
                break;
            case "4":
                replyCustomer(broker);
                break;
            case "5":
                logout(broker);
                break;
        }
    }

    private static void showCEOMenu() {
        System.out.println("\n---CEO Menu---");
        System.out.println("1. View Sales Reports");
        System.out.println("2. Logout");

        String input = scanner.nextLine();

        switch (input) {
            case "1":
                viewSalesReport(broker);
                break;
            case "2":
                logout(broker);
                break;
        }
    }

    private static void initializeSubsystem(AsyncMessageBroker broker) {
        // Database db = Database.getInstance();

        // Subsystems
        account = new AccountManagement();
        account.init(broker);

        item = new ItemManagement();
        item.init(broker);

        message = new Messaging();
        message.init(broker);

        order = new OrderManagement();
        order.init(broker);

        payment = new PaymentService();
        payment.init(broker);

        report = new Reporting();
        report.init(broker);

        wishlist = new WishlistManagement();
        wishlist.init(broker);
    }

    private static void login(AsyncMessageBroker broker) {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        LoginRequest loginRequest = new LoginRequest(username, password);
        broker.publish(EventType.USER_LOGIN_REQUEST, loginRequest);
        System.out.println("Processing login...");
    }

    private static void register(AsyncMessageBroker broker) {
        System.out.println("Enter username:");
        String username = scanner.nextLine();
        System.out.println("Enter email:");
        String email = scanner.nextLine();
        System.out.println("Enter password:");
        String password = scanner.nextLine();

        RegistrationRequest registrationRequest = new RegistrationRequest(username, email, password);
        broker.publish(EventType.USER_REGISTER_REQUESTED, registrationRequest);
        System.out.println("Processing registration...");
    }

    private static void logout(AsyncMessageBroker broker) {
        broker.publish(EventType.USER_LOGOUT_REQUEST, currentUser);
        currentUser = null;
        System.out.println("Logged out successfully");
    }

    private static void browse(AsyncMessageBroker broker) {
        broker.publish(EventType.ITEM_BROWSE_REQUESTED, null);
        System.out.println("Loading items");
    }

    private static void search(AsyncMessageBroker broker) {

    }

    private static void viewWishlist(AsyncMessageBroker broker) {
        broker.publish(EventType.WISHLIST_VIEW_REQUESTED, null);
        System.out.println("Loading wishlist...");
    }

    private static void purchase(AsyncMessageBroker broker) {
        System.out.println("Enter item ID");
        String itemId = scanner.nextLine();
        System.out.println("Enter Quantity");
        String quantity = scanner.nextLine();

        String purchaseRequest = "ItemID:" + itemId + ", Quantity:" + quantity;
        broker.publish(EventType.PURCHASE_REQUESTED, purchaseRequest);
        System.out.println("Processing purchase...");
    }

    private static void viewOrder(AsyncMessageBroker broker) {
        broker.publish(EventType.ORDER_HISTORY_REQUESTED, null);
        System.out.println("Loading orders...");
    }

    private static void sendMessageToStaff(AsyncMessageBroker broker) {
        broker.publish(EventType.MESSAGE_SEND_REQUESTED, null);
        System.out.println("Sending message to staff...");
    }

    private static void viewAccount(AsyncMessageBroker broker) {
        broker.publish(EventType.ACCOUNT_VIEW_REQUESTED, null);
        System.out.println("Loading account information...");
    }

    private static void refill(AsyncMessageBroker broker) {
        System.out.println("Enter item id to fill:");
        String id = scanner.nextLine();
        System.out.println("Enter quantity to add:");
        String quantity = scanner.nextLine();

        String refillRequest = "ItemID:" + id + ", Quantity:" + quantity;
        broker.publish(EventType.ITEM_REFILL_REQUESTED, refillRequest);
        System.out.println("Processing refill...");

    }

    private static void upload(AsyncMessageBroker broker) {
        System.out.println("Enter item name:");
        String name = scanner.nextLine();
        System.out.println("Enter price:");
        String price = scanner.nextLine();
        System.out.println("Enter stock quantity:");
        String stock = scanner.nextLine();

        String uploadRequest = "Name:" + name + ",Price:" + price + ",Stock:" + stock;
        broker.publish(EventType.ITEM_UPLOAD_REQUESTED, uploadRequest);
        System.out.println("Uploading item...");
    }

    private static void editItem(AsyncMessageBroker broker) {
        System.out.println("Enter item id:");
        String id = scanner.nextLine();
        System.out.println("Update item name:");
        String name = scanner.nextLine();
        System.out.println("Update item description:");
        String description = scanner.nextLine();
        System.out.println("Update item price:");
        String price = scanner.nextLine();
        System.out.println("Update item stock quantity:");
        String quantity = scanner.nextLine();

        String editItemRequest = "ID:" + id + ", Name:" + name + ", Description:" + description + ",Price:" + price
                + ",Stock quantity:" + quantity;
        broker.publish(EventType.ITEM_EDIT_REQUESTED, editItemRequest);
        System.out.println("Updating item...");
    }

    private static void replyCustomer(AsyncMessageBroker broker) {
        broker.publish(EventType.MESSAGE_REPLY_REQUESTED, null);
        System.out.println("Replying message to customer...");
    }

    private static void viewSalesReport(AsyncMessageBroker broker) {
        broker.publish(EventType.REPORT_VIEW_REQUESTED, null);
        System.out.println("Fetching report...");
    }
}
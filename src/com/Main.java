package com;

import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.common.Database;
import com.entities.*;
import com.services.*;
import com.repository.*;
import com.subsystems.*;
import com.ui.CeoUI;
import com.ui.CustomerUI;
import com.ui.StaffUI;
import com.ui.UIHelper;
import com.common.dto.account.*;
import com.common.dto.auth.*;
import com.common.dto.item.*;
import com.common.dto.message.*;
import com.common.dto.order.*;
import com.common.dto.payment.*;
import com.common.dto.wishlist.*;
import com.managers.account.*;
import com.managers.item.*;
import com.managers.message.*;
import com.managers.order.*;
import com.managers.payment.*;
import com.managers.report.ReportManager;
import com.managers.wishlist.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.Scanner;
import java.io.Console; // For hidden password input

public class Main {

    public static User currentUser = null;
    private static final Scanner scanner = new Scanner(System.in);

    // Subsystems
    private static AccountManagement account;
    private static ItemManagement item;
    private static Messaging messaging;
    private static OrderManagement order;
    private static PaymentService payment;
    private static Reporting reporting;

    private static WishlistManagement wishlist;

    // Broker + DB
    private static AsyncMessageBroker broker;
    private static Database database;

    // Secret codes for staff/CEO registration
    private static final String STAFF_SECRET_CODE = "STAFF2025";
    private static final String CEO_SECRET_CODE = "CEO2025";

    public static void main(String[] args) {
        UIHelper.clear();
        System.out.println(UIHelper.CYAN + "[Main] Shopping Mall Application Starting..." + UIHelper.RESET);

        // --------------------------------------------------------------
        // 1. Init Broker
        // --------------------------------------------------------------
        broker = new AsyncMessageBroker(1000, 8);
        broker.start();

        // --------------------------------------------------------------
        // 2. Init DB
        // --------------------------------------------------------------
        database = new Database();
        database.connect("jdbc:sqlite:shopping_mall.db");

        // --------------------------------------------------------------
        // 3. Init Repositories + Services
        // --------------------------------------------------------------
        // AuthenticationService has no-arg constructor; create it first
        AuthenticationService authService = new AuthenticationService();

        // SQLiteUserRepository requires Database + AuthenticationService
        UserRepository userRepo = new SQLiteUserRepository(database, authService);

        // Managers for account subsystem
        RegisterManager registerManager = new RegisterManager(userRepo,
                authService);
        LoginManager loginManager = new LoginManager(userRepo, authService);
        EditAccountManager editAccountManager = new EditAccountManager(userRepo, authService);
        ViewAccountManager viewAccountManager = new ViewAccountManager(userRepo);

        // Item repository (persisted in SQLite)
        ItemRepository itemRepo = new SQLiteItemRepository(database);
        ItemRankingRepository itemRankingRepo = new com.repository.InMemoryItemRankingRepository();

        ReportRepository reportRepo = new SQLiteReportRepository(database);
        
        // Order repositories
        OrderRepository orderRepo = new SQLiteOrderRepository(database);
        OrderItemRepository orderItemRepo = new SQLiteOrderItemRepository(database);
        
        ReportManager reportManager = new ReportManager(reportRepo, orderRepo, itemRepo);

        BrowseItemManager browseItemManager = new BrowseItemManager(itemRepo);

        CreateOrderManager createOrderManager = new CreateOrderManager(itemRepo, orderRepo, orderItemRepo);
        EditItemManager editItemManager = new EditItemManager(itemRepo);
        LikeManager likeManager = new LikeManager(itemRepo);
        RankingManager rankingManager = new RankingManager(itemRankingRepo, itemRepo);

        WishlistRepository wishlistRepo = new SQLiteWishlistRepository(database);
        AddWishlistManager addWishlist = new AddWishlistManager(wishlistRepo, itemRepo);
        RemoveWishlistManager removeWishlist = new RemoveWishlistManager(wishlistRepo);
        ViewWishlistManager viewWishlist = new ViewWishlistManager(wishlistRepo, itemRepo);

        // --------------------------------------------------------------
        // 4. Init Subsystems
        // --------------------------------------------------------------
        account = new AccountManagement(registerManager, loginManager, viewAccountManager, editAccountManager);
        item = new ItemManagement(itemRepo);
        messaging = new Messaging();
        order = new OrderManagement(createOrderManager, orderRepo, orderItemRepo, itemRepo);
        payment = new PaymentService();
        reporting = new Reporting(reportManager);
        wishlist = new WishlistManagement(wishlistRepo, itemRepo);

        Subsystems[] modules = {
                account, item, messaging, order,
                payment, reporting, wishlist
        };

        for (Subsystems m : modules)
            m.init(broker);

        // Listen for login success/failure so Main can update currentUser and menus
        broker.registerListener(EventType.USER_LOGIN_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (!(payload instanceof Map))
                return;
            Map<?, ?> map = (Map<?, ?>) payload;
            Object u = map.get("user");
            if (u instanceof User user) {
                currentUser = user;
                System.out.println("[Main] Welcome " + user.getUsername() + " (" + user.getRole() + ")");
                // User logged in successfully - UI menus will handle role-specific display
            }
        }));

        // broker.registerListener(EventType.ITEM_LIST_RETURNED, msg ->
        // CompletableFuture.runAsync(() -> {
        // //if (!VERBOSE_LISTENERS) return;
        // Object payload = msg.getPayload();
        // if (payload instanceof List<?> items) {
        // System.out.println("[Debug] Available items:");
        // for (Object obj : items) {
        // if (obj instanceof Item item) {
        // System.out.println(item.getId() + " | " +
        // item.getName() + " | $" + item.getPrice() +
        // " | stock=" + item.getStockQuantity());
        // }
        // }
        // }
        // }));

        broker.registerListener(EventType.USER_LOGIN_FAILED, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Login failed: " + msg.getPayload());
        }));

        // Handle registration responses
        broker.registerListener(EventType.USER_REGISTER_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Map<?, ?> map) {
                Object user = map.get("user");
                if (user instanceof User u) {
                    System.out.println(UIHelper.GREEN + "Registration successful! Welcome " + u.getUsername() + UIHelper.RESET);
                    System.out.println(UIHelper.GREEN + "You can now login with your credentials." + UIHelper.RESET);
                }
            } else {
                System.out.println(UIHelper.GREEN + "Registration successful! You can now login." + UIHelper.RESET);
            }
            UIHelper.pause();
        }));

        // Handle registration failures
        broker.registerListener(EventType.USER_REGISTER_FAILED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            String errorMsg = payload instanceof String ? (String) payload : "Registration failed. Please try again.";
            System.out.println(UIHelper.RED + "Registration failed: " + errorMsg + UIHelper.RESET);
            UIHelper.pause();
        }));

        // Handle wishlist responses
        broker.registerListener(EventType.WISHLIST_DETAILS_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof List<?> items) {
                if (items.isEmpty()) {
                    System.out.println("Your wishlist is empty.");
                } else {
                    System.out.println("=== Your Wishlist ===");
                    for (Object obj : items) {
                        if (obj instanceof Item item) {
                            System.out.println(item.getId() + " | " + item.getName() +
                                    " | $" + item.getPrice() + " | stock=" + item.getStockQuantity());
                        }
                    }
                    System.out.println("=====================");
                }
            }
        }));

        broker.registerListener(EventType.WISHLIST_ADD_SUCCESS, msg -> {
            System.out.println("Item added to wishlist successfully");
            return CompletableFuture.completedFuture(null);
        });

        broker.registerListener(EventType.WISHLIST_ADD_FAILED, msg -> {
            System.out.println("Failed to add item: " + msg.getPayload());
            return CompletableFuture.completedFuture(null);
        });

        broker.registerListener(EventType.WISHLIST_REMOVE_SUCCESS, msg -> {
            System.out.println("Item removed from wishlist successfully");
            return CompletableFuture.completedFuture(null);
        });

        broker.registerListener(EventType.WISHLIST_REMOVE_FAILED, msg -> {
            System.out.println("Failed to remove item: " + msg.getPayload());
            return CompletableFuture.completedFuture(null);
        });

        // Handle purchase/order responses
        broker.registerListener(EventType.ORDER_CONFIRMED, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Purchase completed successfully!");
            Object payload = msg.getPayload();
            if (payload instanceof Order order) {
                System.out.println("Order ID: " + order.getId());
                System.out.println("Total: $" + order.getTotalAmount());
                System.out.println("Status: " + order.getStatus());
            }
        }));

        // // Handle order history responses
        // broker.registerListener(EventType.ORDER_HISTORY_RETURNED, msg ->
        // CompletableFuture.runAsync(() -> {
        // Object payload = msg.getPayload();
        // if (payload instanceof List<?> orders) {
        // if (orders.isEmpty()) {
        // System.out.println("You have no order history.");
        // } else {
        // System.out.println("=== Your Order History ===");
        // for (Object obj : orders) {
        // if (obj instanceof Order order) {
        // System.out.println("Order ID: " + order.getId() +
        // " | Date: " + order.getOrderDate() +
        // " | Total: $" + order.getTotalAmount() +
        // " | Status: " + order.getStatus());
        // }
        // }
        // System.out.println("==========================");
        // }
        // }
        // }));

        // Handle account view responses
        broker.registerListener(EventType.ACCOUNT_VIEW_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof User user) {
                System.out.println("=== Account Information ===");
                System.out.println("Username: " + user.getUsername());
                System.out.println("Email: " + user.getEmail());
                System.out.println("Role: " + user.getRole());
                System.out.println("Phone Number: " + user.getPhoneNumber());
                System.out.println("Address: " + user.getAddress());
                System.out.println("===========================");
            } else {
                System.out.println("[Main] Unable to retrieve account information.");
            }
        }));

        // Handle account update success - update Main.currentUser
        broker.registerListener(EventType.ACCOUNT_UPDATE_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof User updatedUser) {
                // Update the current user session with the new data
                if (currentUser != null && currentUser.getId() == updatedUser.getId()) {
                    currentUser = updatedUser;
                    System.out.println("[Main] Current user session updated: " + updatedUser.getUsername());
                }
            }
        }));

        // Handle account update failures
        broker.registerListener(EventType.ACCOUNT_UPDATE_FAILED, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Account update failed: " + msg.getPayload());
        }));

        // Handle messaging responses
        broker.registerListener(EventType.MESSAGE_SENT_CONFIRMATION, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Message sent successfully!");
        }));

        broker.registerListener(EventType.MESSAGE_LIST_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof List<?> messages) {
                if (messages.isEmpty()) {
                    System.out.println("No messages found.");
                } else {
                    System.out.println("=== Messages ===");
                    for (Object obj : messages) {
                        if (obj instanceof UserMessage message) {
                            System.out.println("From: " + message.getSenderId() +
                                    " | Subject: " + message.getSubject() +
                                    " | Date: " + message.getTimeStamp());
                            System.out.println("Message: " + message.getContent());
                            System.out.println("----------------");
                        }
                    }
                    System.out.println("================");
                }
            }
        }));

        // Handle conversation responses - REMOVED to prevent duplication
        // Conversations are now displayed directly in CustomerUI and StaffUI
        // broker.registerListener(EventType.CONVERSATION_LIST_RETURNED, msg -> CompletableFuture.runAsync(() -> {
        //     Object payload = msg.getPayload();
        //     if (payload instanceof List<?> conversations) {
        //         if (conversations.isEmpty()) {
        //             System.out.println("No conversations found.");
        //         } else {
        //             System.out.println("=== Conversations ===");
        //             for (Object obj : conversations) {
        //                 if (obj instanceof Conversation conv) {
        //                     if (currentUser.getRole() == User.Role.CUSTOMER) {
        //                         // Customer sees staff info
        //                         System.out.println("With: " + conv.getStaffName() +
        //                                 " | Last: " + conv.getLastMessage() +
        //                                 " | Unread: " + conv.getUnreadCount());
        //                     } else {
        //                         // Staff sees customer ID clearly for replying
        //                         System.out.println("Customer ID: " + conv.getCustomerId() +
        //                                 " (" + conv.getCustomerName() + ")" +
        //                                 " | Last: " + conv.getLastMessage() +
        //                                 " | Unread: " + conv.getUnreadCount());
        //                     }
        //                 }
        //             }
        //         }
        //         UIHelper.pause();
        //     }
        // }));

        broker.registerListener(EventType.CONVERSATION_MESSAGES_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof List<?> messages) {
                if (messages.isEmpty()) {
                    System.out.println("No messages in this conversation.");
                } else {
                    System.out.println("=== Conversation Messages ===");
                    for (Object obj : messages) {
                        if (obj instanceof UserMessage message) {
                            String senderType = message.getSenderId() == currentUser.getId() ? "You" : "Other";
                            System.out.println("[" + senderType + "] " + message.getContent());
                            System.out.println("Time: " + new java.util.Date(message.getTimeStamp()));
                            System.out.println("Status: " + message.getStatus());
                            System.out.println("---");
                        }
                    }
                    System.out.println("=============================");
                }
            }
        }));

        broker.registerListener(EventType.UNREAD_MESSAGES_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof List<?> messages) {
                if (messages.isEmpty()) {
                    System.out.println("No unread messages.");
                } else {
                    System.out.println("=== Unread Messages (" + messages.size() + ") ===");
                    for (Object obj : messages) {
                        if (obj instanceof UserMessage message) {
                            System.out.println("From Customer ID: " + message.getSenderId());
                            System.out.println("Subject: " + message.getSubject());
                            System.out.println("Message: " + message.getContent());
                            System.out.println("Time: " + new java.util.Date(message.getTimeStamp()));
                            System.out.println("---");
                        }
                    }
                    System.out.println("===============================");
                }
            }
        }));

        broker.registerListener(EventType.STAFF_NOTIFIED_NEW_MESSAGE, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof UserMessage message) {
                if (currentUser != null && currentUser.getRole() == User.Role.STAFF) {
                    System.out.println("\nNew message from Customer ID: " + message.getSenderId());
                    System.out.println("Subject: " + message.getSubject());
                    System.out.println("Preview: " + message.getContent().substring(0,
                            Math.min(50, message.getContent().length())) + "...");
                }
            }
        }));

        broker.registerListener(EventType.CUSTOMER_NOTIFIED_NEW_REPLY, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof UserMessage message) {
                if (currentUser != null && currentUser.getId() == message.getRecipientId()) {
                    System.out.println("\nNew reply from staff!");
                    System.out.println("Message: " + message.getContent());
                }
            }
        }));

        // Handle item management responses (for staff)
        broker.registerListener(EventType.ITEM_UPDATE_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Item operation completed successfully!");
            Object payload = msg.getPayload();
            if (payload instanceof Item item) {
                System.out.println("Item: " + item.getName() + " | Stock: " + item.getStockQuantity());
            }
        }));

        // Handle reporting responses (for CEO)
        broker.registerListener(EventType.REPORT_DETAILS_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Report report) {
                System.out.println("=== Sales Report ===");
                System.out.println("Report ID: " + report.getId());
                System.out.println("Period: " + report.getReportType());
                System.out.println("Generated: " + report.getDateGenerated());
                System.out.println("Content: " + report.getReportData());
                System.out.println("====================");
            } else {
                System.out.println("Sales report data: " + payload);
            }
        }));

        // Add event listener for report completion:
        broker.registerListener(EventType.REPORT_GENERATION_COMPLETE, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Report report) {
                System.out.println("=== REPORT GENERATED ===");
                System.out.println("ID: " + report.getId());
                System.out.println("Type: " + report.getReportType());
                System.out.println("Generated: " + new java.util.Date(report.getDateGenerated()));
                System.out.println("Period Start: " + new java.util.Date(report.getDataStart()));
                System.out.println("");
                System.out.println("FULL REPORT DATA:");
                System.out.println(report.getReportData());
                System.out.println("========================");
            }
        }));

        // Handle notification responses
        broker.registerListener(EventType.NOTIFICATION_SENT, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Notification] " + msg.getPayload());
        }));

        broker.registerListener(EventType.NOTIFICATION_FAILED, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Notification Failed] " + msg.getPayload());
        }));

        // --------------------------------------------------------------
        // 5. Main menu loop
        // --------------------------------------------------------------
        while (true) {
            if (currentUser == null) {
                showGuestMenu();
            } else {
                switch (currentUser.getRole()) {
                    case CUSTOMER -> CustomerUI.showMenu(scanner, broker);
                    case STAFF -> StaffUI.showMenu(scanner, broker);
                    case CEO -> CeoUI.showMenu(scanner, broker);
                }
            }
        }
        // Note: shutdown is called from individual UI menus when user quits
    }

    // ============================================================
    // MENU HANDLERS
    // ============================================================
    private static void showGuestMenu() {
        UIHelper.clear();

        UIHelper.box(
                UIHelper.color("WELCOME TO SHOPPING MALL APP", UIHelper.BLUE),
                List.of(
                        "1. Login",
                        "2. Register",
                        "3. Browse Items",
                        "Q. Quit"));
        System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);

        String input = scanner.nextLine();

        switch (input) {
            case "1" -> login();
            case "2" -> register();
            case "3" -> CustomerUI.browse(scanner, broker);
            case "Q", "q" ->
                shutdown(new Subsystems[] { account, item, messaging, order, payment, reporting, wishlist });
            default -> {
                System.out.println(UIHelper.RED + "Invalid option!" + UIHelper.RESET);
                UIHelper.pause();
            }
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    /**
     * Reads password input with hidden characters (no echo) in terminal.
     * Uses Console.readPassword() which works in real terminals.
     * Falls back to Scanner if console is not available (e.g., in some IDEs).
     * 
     * IMPORTANT: For hidden password input, run the application from a terminal/command prompt.
     * When running from an IDE, passwords may be visible due to console limitations.
     * 
     * @return The password string entered by the user
     */
    public static String readPasswordHidden() {
        Console console = System.console();
        if (console != null) {
            // Use Console.readPassword() - this hides input in terminal
            char[] password = console.readPassword();
            return password != null ? new String(password) : "";
        } else {
            // Fallback: if console is not available (e.g., in IDEs), use Scanner
            // Note: Password will be visible when typing - this is a limitation when
            // running in IDEs that don't provide a real console.
            // For hidden passwords, run from: cmd.exe, PowerShell, or terminal
            return scanner.nextLine().trim();
        }
    }

    private static void login() {
        System.out.println(UIHelper.BLUE + "--- LOGIN ---" + UIHelper.RESET);
        System.out.print("Username/Email: ");
        String name = scanner.nextLine().trim();

        System.out.print("Password: ");
        String pw = readPasswordHidden();

        broker.publish(EventType.USER_LOGIN_REQUEST,
                new LoginRequest(name, pw));
    }

    private static void register() {
        UIHelper.box(
                UIHelper.color("REGISTER ACCOUNT", UIHelper.GREEN),
                List.of(
                        "1. Customer",
                        "2. Staff",
                        "3. CEO",
                        "4. Back"));
        System.out.print(UIHelper.YELLOW + "Choose your role: " + UIHelper.RESET);
        String input = scanner.nextLine().trim();

        switch (input) {
            case "1" -> {
                requestCredentials(User.Role.CUSTOMER, "Customer");
            }
            case "2" -> registerWithSecret(User.Role.STAFF, "Staff", STAFF_SECRET_CODE);
            case "3" -> registerWithSecret(User.Role.CEO, "CEO", CEO_SECRET_CODE);
            case "4" -> {
                return;
            }
            default -> {
                System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
                UIHelper.pause();
            }
        }
    }

    private static void registerWithSecret(User.Role role, String label, String secretCode) {
        System.out.print(label + " Secret Code: ");
        String provided = scanner.nextLine().trim();
        if (!secretCode.equalsIgnoreCase(provided)) {
            System.out.println(UIHelper.RED + "Incorrect secret code." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        requestCredentials(role, label);
    }

    private static void requestCredentials(User.Role role, String label) {
        System.out.print("Choose username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = readPasswordHidden();

        System.out.print("Phone: ");
        String phone = scanner.nextLine().trim();

        System.out.print("Address: ");
        String address = scanner.nextLine().trim();

        broker.publish(EventType.USER_REGISTER_REQUESTED,
                new RegistrationRequest(username, email, password, role, phone, address));
    }

    // ============================================================
    // SHUTDOWN
    // ============================================================
    private static void shutdown(Subsystems[] modules) {
        System.out.println(UIHelper.RED + "[System] Shutting down..." + UIHelper.RESET);

        for (Subsystems m : modules)
            m.shutdown();

        scanner.close();
        database.close();
        broker.stop();
        System.out.println(UIHelper.GREEN + "[System] Application terminated." + UIHelper.RESET);
        System.exit(0);
    }
}
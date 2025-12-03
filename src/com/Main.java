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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class Main {

    // ============================================================
    // STATE
    // ============================================================

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static User currentUser = null;
    private static final Scanner scanner = new Scanner(System.in);

    private static boolean selectingItem = false;
    private static boolean inWishlistMenu = false;
    private static boolean inPaymentCardMenu = false;
    private static boolean inNotificationMenu = false;

    private static int selectedItemId = -1;
    private static String lastMenu = "GUEST";
    private static OrderCreateRequest pendingOrderRequest = null;
    
    // Notification system
    private static final List<String> notifications = new ArrayList<>();
    
    public static List<String> getNotifications() {
        return new ArrayList<>(notifications);
    }
    
    public static void addNotification(String message) {
        notifications.add(message);
        System.out.println("[Main] Notification added: " + message);
    }
    
    public static void clearNotifications() {
        notifications.clear();
        System.out.println("[Main] Notifications cleared");
    }

    // ============================================================
    // CORE SERVICES
    // ============================================================

    private static AsyncMessageBroker broker;
    private static Database database;

    // Subsystems
    private static AccountManagement account;
    private static ItemManagement item;
    private static Messaging messaging;
    private static OrderManagement orderSubsystem;
    private static PaymentManagement paymentSubsystem;
    private static Reporting reporting;
    private static WishlistManagement wishlistSubsystem;
    private static ItemRepository itemRepository;
    private static OrderRepository orderRepository;
    private static OrderItemRepository orderItemRepository;

    // Payment Managers (Option A)
    private static PaymentCardManager paymentCardManager;
    private static PaymentAuthorizationManager paymentAuthManager;
    private static PaymentTransactionManager paymentTxnManager;
    private static PaymentControllerManager paymentController;

    // Secret codes for staff/CEO registration
    private static final String STAFF_SECRET_CODE = "STAFF2025";
    private static final String CEO_SECRET_CODE = "CEO2025";

    // ============================================================
    // MAIN
    // ============================================================

    public static void main(String[] args) {
        UIHelper.clear();
        System.out.println(UIHelper.CYAN + "[Main] Shopping Mall Application Starting..." + UIHelper.RESET);

        // ------------------------------------------------------------
        // INIT BROKER
        // ------------------------------------------------------------
        broker = new AsyncMessageBroker(1000, 8);
        broker.start();

        // ------------------------------------------------------------
        // INIT DATABASE
        // ------------------------------------------------------------
        database = new Database();
        database.connect("jdbc:sqlite:shopping_mall.db");

        // ------------------------------------------------------------
        // INIT REPOSITORIES
        // ------------------------------------------------------------
        AuthenticationService authService = new AuthenticationService();

        UserRepository userRepo = new SQLiteUserRepository(database, authService);
        itemRepository = new SQLiteItemRepository(database);
        ItemRankingRepository rankingRepo = new InMemoryItemRankingRepository();
        WishlistRepository wishlistRepo = new SQLiteWishlistRepository(database);
        PaymentCardRepository cardRepo = new SQLitePaymentCardRepository(database);

        orderRepository = new SQLiteOrderRepository(database);
        orderItemRepository = new SQLiteOrderItemRepository(database);

        // ------------------------------------------------------------
        // INIT MANAGERS
        // ------------------------------------------------------------

        RegisterManager registerMgr = new RegisterManager(userRepo, authService);
        LoginManager loginMgr = new LoginManager(userRepo, authService);
        ViewAccountManager viewAccountMgr = new ViewAccountManager(userRepo);
        EditAccountManager editAccountMgr = new EditAccountManager(userRepo, authService);

        BrowseItemManager browseManager = new BrowseItemManager(itemRepository);
        EditItemManager editItemManager = new EditItemManager(itemRepository);
        LikeManager likeManager = new LikeManager(itemRepository);
        RankingManager rankingManager = new RankingManager(rankingRepo, itemRepository);

        // Payment managers
        paymentCardManager = new PaymentCardManager(cardRepo);
        // Note: For now, we'll only initialize the payment card manager
        // Other managers require PaymentTransactionRepository which needs more setup

        // Payment controller (requires full payment manager setup)
        // paymentController = new PaymentControllerManager(
        //     paymentCardManager,
        //     paymentAuthManager,
        //     paymentTxnManager,
        //     receiptMgr,
        //     broker);

        // ------------------------------------------------------------
        // INIT SUBSYSTEMS
        // ------------------------------------------------------------

        account = new AccountManagement(registerMgr, loginMgr, viewAccountMgr, editAccountMgr);
        item = new ItemManagement(itemRepository);
        messaging = new Messaging(database);
        // Initialize CreateOrderManager first
        CreateOrderManager createOrderMgr = new CreateOrderManager(itemRepository, orderRepository,
                orderItemRepository);
        orderSubsystem = new OrderManagement(
                createOrderMgr,
                orderRepository,
                orderItemRepository,
                itemRepository);
        paymentSubsystem = new PaymentManagement();
        // Initialize report manager and reporting subsystem
        ReportManager reportManager = new ReportManager(new SQLiteReportRepository(database), orderRepository,
                itemRepository);
        reporting = new Reporting(reportManager);
        wishlistSubsystem = new WishlistManagement(wishlistRepo, itemRepository);

        Subsystems[] modules = {
                account, item, messaging, orderSubsystem,
                paymentSubsystem,
                reporting, wishlistSubsystem
        };

        for (Subsystems m : modules)
            m.init(broker);

        // ============================================================
        // EVENT LISTENERS
        // ============================================================
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
                switch (user.getRole()) {
                    // case CUSTOMER -> lastMenu = "CUSTOMER";
                    // case STAFF -> lastMenu = "STAFF";
                    // case CEO -> lastMenu = "CEO";
                }
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

        broker.registerListener(EventType.USER_LOGIN_FAILED, msg -> CompletableFuture.runAsync(() ->

        {
            System.out.println("[Main] Login failed: " + msg.getPayload());
        }));

        // Handle registration responses
        broker.registerListener(EventType.USER_REGISTER_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Map<?, ?> map) {
                Object user = map.get("user");
                if (user instanceof User u) {
                    System.out.println("[Main] Registration successful! Welcome " + u.getUsername());
                }
            } else {
                System.out.println("[Main] You can now login.");
            }
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
                        if (obj instanceof Wishlist wishlistItem) {
                            // Fetch item details from repository
                            Item item = itemRepository.findById(wishlistItem.getItemId());
                            if (item != null) {
                                System.out.println(item.getId() + " | " + item.getName() +
                                        " | $" + item.getPrice() + " | stock=" + item.getStockQuantity());
                            } else {
                                System.out.println("Item #" + wishlistItem.getItemId() + " (details unavailable)");
                            }
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

        // Handle order history responses
        broker.registerListener(EventType.ORDER_HISTORY_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof List<?> orders) {
                if (orders.isEmpty()) {
                    System.out.println("You have no order history.");
                } else {
                    System.out.println("=== Your Order History ===");
                    for (Object obj : orders) {
                        if (obj instanceof Order order) {
                            System.out.println("Order ID: " + order.getId() +
                                    " | Date: " + order.getOrderDate() +
                                    " | Total: $" + order.getTotalAmount() +
                                    " | Status: " + order.getStatus());
                        }
                    }
                    System.out.println("========================");
                }
            }
        }));
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

        // Handle account update responses
        broker.registerListener(EventType.ACCOUNT_UPDATE_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof User user) {
                currentUser = user; // Update current user
                System.out.println("[Main] Account updated successfully!");
            }
        }));

        broker.registerListener(EventType.ACCOUNT_UPDATE_FAILED, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Account update failed: " + msg.getPayload());
        }));

        // Handle payment card responses
        broker.registerListener(EventType.PAYMENT_CARD_ADD_REQUESTED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof com.common.dto.payment.PaymentCardAddRequest request) {
                try {
                    // Create new payment card
                    PaymentCard newCard = new PaymentCard(
                        0, // ID will be generated
                        request.getUserId(),
                        request.getCardType(),
                        request.getCardNumber(), // Will be masked in repository
                        request.getExpiryDate(),
                        request.getCardHolderName(),
                        false
                    );
                    
                    paymentCardManager.addNewCard(newCard);
                    System.out.println("[Main] Payment card added successfully for user " + request.getUserId());
                } catch (Exception e) {
                    System.out.println("[Main] Failed to add payment card: " + e.getMessage());
                }
            }
        }));

        broker.registerListener(EventType.PAYMENT_CARD_EDIT_REQUESTED, msg -> CompletableFuture.runAsync(() -> {
            System.out.println("[Main] Payment card edit request received (edit functionality to be implemented)");
        }));

        broker.registerListener(EventType.PAYMENT_CARD_LIST_REQUESTED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Integer userId) {
                try {
                    List<PaymentCard> cards = paymentCardManager.getCardsForUser(userId);
                    broker.publish(EventType.PAYMENT_CARD_LIST_RETURNED, cards);
                    System.out.println("[Main] Found " + cards.size() + " payment cards for user " + userId);
                } catch (Exception e) {
                    System.out.println("[Main] Failed to list payment cards: " + e.getMessage());
                    broker.publish(EventType.PAYMENT_CARD_LIST_RETURNED, new ArrayList<>());
                }
            }
        }));
        
        // Notification handlers for orders and purchases
        broker.registerListener(EventType.ORDER_CONFIRMED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Order order) {
                String message = String.format("Order #%d confirmed successfully! Total: $%.2f", 
                    order.getId(), order.getTotalAmount());
                addNotification(message);
                System.out.println("[Main] Enhanced order confirmation: " + message);
            } else {
                addNotification("Order confirmed successfully!");
                System.out.println("[Main] Basic order confirmation notification added");
            }
        }));
        
        broker.registerListener(EventType.PAYMENT_AUTHORIZED, msg -> CompletableFuture.runAsync(() -> {
            addNotification("Payment authorized - your order is being processed");
            System.out.println("[Main] Payment authorization notification added");
        }));
        
        broker.registerListener(EventType.PAYMENT_DENIED, msg -> CompletableFuture.runAsync(() -> {
            addNotification("Payment was denied. Please check your payment method.");
            System.out.println("[Main] Payment denial notification added");
        }));
        
        // Handler for item updates (likes, stock changes)
        broker.registerListener(EventType.ITEM_UPDATE_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Item item) {
                System.out.println("[Main] Item updated: " + item.getName() + " (Likes: " + item.getLikeCount() + ", Stock: " + item.getStockQuantity() + ")");
            }
        }));
        
        // Handler for wishlist operations
        broker.registerListener(EventType.WISHLIST_ADD_SUCCESS, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof Item item) {
                addNotification("Added '" + item.getName() + "' to your wishlist");
                System.out.println("[Main] Wishlist add success: " + item.getName());
            } else {
                System.out.println("[Main] Wishlist add success (generic)");
            }
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

        // Handle conversation responses
        broker.registerListener(EventType.CONVERSATION_LIST_RETURNED, msg -> CompletableFuture.runAsync(() -> {
            Object payload = msg.getPayload();
            if (payload instanceof List<?> conversations) {
                if (conversations.isEmpty()) {
                    System.out.println("No conversations found.");
                } else {
                    System.out.println("=== Conversations ===");
                    for (Object obj : conversations) {
                        if (obj instanceof Conversation conv) {
                            if (currentUser.getRole() == User.Role.CUSTOMER) {
                                // Customer sees staff info
                                System.out.println("With: " + conv.getStaffName() +
                                        " | Last: " + conv.getLastMessage() +
                                        " | Unread: " + conv.getUnreadCount());
                            } else {
                                // Staff sees customer ID clearly for replying
                                System.out.println("Customer ID: " + conv.getCustomerId() +
                                        " (" + conv.getCustomerName() + ")" +
                                        " | Last: " + conv.getLastMessage() +
                                        " | Unread: " + conv.getUnreadCount());
                            }
                        }
                    }
                }
                UIHelper.pause();
            }
        }));

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
                    System.out.println("\n🔔 New message from Customer ID: " + message.getSenderId());
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
                    System.out.println("\n💬 New reply from staff!");
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

        // Add notification system for customer notifications
        broker.registerListener(EventType.ORDER_CONFIRMED, msg -> CompletableFuture.runAsync(() -> {
            if (currentUser != null) {
                notifications.add("Order confirmed successfully! Check 'View My Orders' for details.");
                System.out.println("[Main] Notification added: Order confirmed");
            }
        }));
        
        broker.registerListener(EventType.MESSAGE_SENT_CONFIRMATION, msg -> CompletableFuture.runAsync(() -> {
            if (currentUser != null) {
                notifications.add("New message from staff: " + msg.getPayload());
                System.out.println("[Main] Notification added: Message from staff");
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
        // 5. Main loop
        // --------------------------------------------------------------
        while (true) {
            UIHelper.clear();
            if (currentUser == null)
                showGuestMenu();
            else {
                switch (currentUser.getRole()) {
                    case CUSTOMER -> CustomerUI.showMenu(scanner, broker);
                    case STAFF -> StaffUI.showMenu(scanner, broker);
                    case CEO -> CeoUI.showMenu(scanner, broker);
                }
            }

            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("Q"))
                break;
        }
        shutdown(modules);
    }

    // ============================================================
    // MENU HANDLERS
    // ============================================================
    private static void showGuestMenu() {
        UIHelper.box(
                UIHelper.color("     WELCOME TO SHOPPING MALL APP", UIHelper.BLUE),
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
            case "4" -> shutdown(new Subsystems[] {});
            default -> System.out.println(UIHelper.RED + "[Error] Invalid selection!" + UIHelper.RESET);
        }
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    public static String readPasswordHidden() {
        Console console = System.console();
        if (console != null) {
            // If console is available, use it to hide password input
            char[] password = console.readPassword();
            return password != null ? new String(password) : "";
        } else {
            // Fallback: if console is not available (e.g., in IDEs), use Scanner
            // In this case, password will be visible - this is a limitation in some
            // environments
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

        System.out.println(UIHelper.GREEN + label + " account created successfully. Please log in." + UIHelper.RESET);
        UIHelper.pause();
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
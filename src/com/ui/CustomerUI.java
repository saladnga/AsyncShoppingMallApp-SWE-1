package com.ui;

import com.Main;
import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.common.dto.message.ConversationListRequest;
import com.common.dto.message.MessageSendRequest;
import com.common.dto.order.OrderCreateRequest;
import com.common.dto.order.OrderCreateRequest.OrderItemRequest;
import com.common.dto.wishlist.WishlistAddRequest;
import com.common.dto.wishlist.WishlistRemoveRequest;
import com.common.dto.item.ItemLikeRequest;
import com.entities.Item;
import com.entities.Order;
import com.entities.PaymentCard;
import com.entities.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CustomerUI {

    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void showMenu(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.clear();

        UIHelper.box(
                UIHelper.color("CUSTOMER MENU", UIHelper.CYAN),
                List.of(
                        "1. Browse Items",
                        "2. Search Items",
                        "3. View Wishlist",
                        "4. View My Orders",
                        "5. Send Message to Staff",
                        "6. View Notifications",
                        "7. View Account",
                        "8. Logout",
                        "Q. Quit"));

        System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);
        String input = scanner.nextLine();

        switch (input) {
            case "1" -> browse(scanner, broker);
            case "2" -> search(scanner, broker);
            case "3" -> viewWishlist(scanner, broker);
            case "4" -> viewOrders(scanner, broker);
            case "5" -> sendMessage(scanner, broker);
            case "6" -> showNotificationMenu(scanner, broker);
            case "7" -> viewAccount(scanner, broker);
            case "8" -> Main.currentUser = null;
            case "Q", "q" -> System.exit(0);
            default -> System.out.println(UIHelper.RED + "Invalid option!" + UIHelper.RESET);
        }
    }

    public static void browse(Scanner scanner, AsyncMessageBroker broker) {
        // broker.publish(EventType.ITEM_BROWSE_REQUESTED, new ItemBrowseRequest());

        List<Item> items = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                EventType.ITEM_LIST_RETURNED, 3000);

        if (items == null || items.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No items available." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        System.out.println(UIHelper.CYAN + "--- ALL ITEMS ---" + UIHelper.RESET);
        for (Item i : items) {
            List<String> boxLines = List.of(
                    String.format("Description: %s", i.getDescription()),
                    String.format("Price: $%.2f", i.getPrice()),
                    String.format("Stock: %d available", i.getStockQuantity()),
                    String.format("Likes: %d", i.getLikeCount()));

            UIHelper.box(
                    UIHelper.color(String.format("#%d %s", i.getId(), i.getName()), UIHelper.GREEN),
                    boxLines);
        }

        while (true) {
            System.out.println(UIHelper.YELLOW + "Actions:" + UIHelper.RESET);
            System.out.println("1. Add to Wishlist");
            System.out.println("2. Like Item");
            System.out.println("3. Buy Now");
            System.out.println("4. Back to main menu");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    if (!requireLoggedInForAction("add items to a wishlist")) {
                        continue;
                    }
                    System.out.print("Enter Item ID to add to Wishlist: ");
                    String idInput = scanner.nextLine().trim();
                    Integer itemId = parseItemId(idInput);
                    if (itemId == null)
                        continue;

                    Item selected = items.stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                    if (selected == null) {
                        System.out.println(UIHelper.RED + "Item not found." + UIHelper.RESET);
                        continue;
                    }

                    broker.publish(EventType.WISHLIST_ADD_REQUESTED,
                            new WishlistAddRequest(Main.currentUser.getId(), itemId, 1));

                    System.out.println(
                            UIHelper.GREEN + "Added '" + selected.getName() + "' to your wishlist." + UIHelper.RESET);
                    UIHelper.pause();
                }

                case "2" -> {
                    if (!requireLoggedInForAction("like an item")) {
                        continue;
                    }
                    System.out.print("Enter ItemID to like: ");
                    String idInput = scanner.nextLine().trim();
                    Integer itemId = parseItemId(idInput);
                    if (itemId == null)
                        continue;

                    Item selected = items.stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                    if (selected == null) {
                        System.out.println(UIHelper.RED + "Item not found." + UIHelper.RESET);
                        continue;
                    }

                    broker.publish(EventType.ITEM_LIKE_REQUESTED,
                            new ItemLikeRequest(Main.currentUser.getId(), itemId));
                    System.out.println(UIHelper.GREEN + "You liked '" + selected.getName() + "'." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "3" -> {
                    if (!requireLoggedInForAction("purchase an item")) {
                        continue;
                    }
                    purchase(scanner, broker);
                    UIHelper.pause();
                    return;
                }
                case "4" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    private static boolean purchase(Scanner scanner, AsyncMessageBroker broker) {
        System.out.print("Enter Item ID to purchase: ");
        int itemId = Integer.parseInt(scanner.nextLine());

        // Fetch item info
        List<Item> items = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                EventType.ITEM_LIST_RETURNED, 3000);
        Item item = null;
        if (items != null) {
            item = items.stream().filter(i -> i.getId() == itemId).findFirst().orElse(null);
        }

        if (item == null) {
            System.out.println(UIHelper.RED + "Invalid item." + UIHelper.RESET);
            return false;
        }

        System.out.println(UIHelper.CYAN + "--- ITEM DETAILS ---" + UIHelper.RESET);
        System.out.println("Name: " + item.getName());
        System.out.println("Description: " + item.getDescription());
        System.out.printf("Price: $%.2f%n", item.getPrice());
        System.out.println("Likes: " + item.getLikeCount());

        while (true) {
            System.out.print("Enter quantity to purchase: ");
            int quantity = Integer.parseInt(scanner.nextLine());

            if (quantity > item.getStockQuantity()) {
                System.out.println(UIHelper.RED + "Insufficient stock available." + UIHelper.RESET);
                continue;
            }

            double total = item.getPrice() * quantity;
            System.out.printf("Total Amount: $%.2f%n", total);

            while (true) {
                System.out.println("1. Confirm purchase");
                System.out.println("2. Cancel purchase");
                System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);
                String confirm = scanner.nextLine().trim();

                if ("1".equals(confirm)) {
                    // Create OrderCreateRequest and publish (older flow uses PURCHASE_REQUESTED
                    // with OrderCreateRequest)
                    OrderItemRequest orderItem = new OrderItemRequest(itemId, quantity);
                    OrderCreateRequest req = new OrderCreateRequest(Main.currentUser.getId(), List.of(orderItem),
                            "Default Address");
                    broker.publish(EventType.PURCHASE_REQUESTED, req);

                    UIHelper.loading("Processing payment");
                    System.out.println(UIHelper.GREEN + "Purchase requested. Check notifications for confirmation."
                            + UIHelper.RESET);
                    return true;
                } else if ("2".equals(confirm)) {
                    System.out.println(UIHelper.YELLOW + "Purchase canceled." + UIHelper.RESET);
                    return false;
                } else {
                    System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
                }
            }
        }
    }

    private static Integer parseItemId(String idInput) {
        return parsePositiveInt(idInput, "item ID");
    }

    private static Integer parsePositiveInt(String input, String label) {
        try {
            int parsed = Integer.parseInt(input);
            if (parsed <= 0)
                throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid " + label + "." + UIHelper.RESET);
            return null;
        }
    }

    private static boolean requireLoggedInForAction(String action) {
        if (Main.currentUser != null) {
            return true;
        }
        System.out.println(UIHelper.RED + "Please log in to " + action + "." + UIHelper.RESET);
        return false;
    }

    public static void search(Scanner scanner, AsyncMessageBroker broker) {
        System.out.print("Enter keywords: ");
        String keyword = scanner.nextLine();

        List<Item> result = BrokerUtils.requestOnce(broker, EventType.ITEM_SEARCH_REQUESTED, keyword,
                EventType.ITEM_LIST_RETURNED, 3000);

        System.out.println(UIHelper.CYAN + "--- SEARCH RESULTS ---" + UIHelper.RESET);
        if (result == null || result.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No items found." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        for (Item i : result) {
            List<String> boxLines = List.of(
                    String.format("Description: %s", i.getDescription()),
                    String.format("Price: $%.2f", i.getPrice()),
                    String.format("Stock: %d available", i.getStockQuantity()),
                    String.format("Likes: %d", i.getLikeCount()));

            UIHelper.box(
                    UIHelper.color(String.format("#%d %s", i.getId(), i.getName()), UIHelper.GREEN),
                    boxLines);
        }

        while (true) {
            System.out.println(UIHelper.YELLOW + "Actions:" + UIHelper.RESET);
            System.out.println("1. Add to Wishlist");
            System.out.println("2. Like Item");
            System.out.println("3. Buy Now");
            System.out.println("4. Back to main menu");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    if (!requireLoggedInForAction("add items to a wishlist")) {
                        continue;
                    }

                    System.out.print("Enter Item ID to add to Wishlist: ");
                    String idInput = scanner.nextLine().trim();
                    Integer itemId = parseItemId(idInput);

                    if (itemId == null)
                        continue;

                    Item selected = result.stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                    if (selected == null) {
                        System.out.println(UIHelper.RED + "Item not found." + UIHelper.RESET);
                        continue;
                    }

                    broker.publish(EventType.WISHLIST_ADD_REQUESTED,
                            new WishlistAddRequest(Main.currentUser.getId(), itemId, 1));

                    System.out.println(
                            UIHelper.GREEN + "Added '" + selected.getName() + "' to your wishlist." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "2" -> {
                    if (!requireLoggedInForAction("like an item")) {
                        continue;
                    }
                    System.out.print("Enter Item ID to like: ");
                    String idInput = scanner.nextLine().trim();
                    Integer itemId = parseItemId(idInput);
                    if (itemId == null)
                        continue;

                    Item selected = result.stream().filter(it -> it.getId() == itemId).findFirst().orElse(null);
                    if (selected == null) {
                        System.out.println(UIHelper.RED + "Item not found." + UIHelper.RESET);
                        continue;
                    }

                    broker.publish(EventType.ITEM_LIKE_REQUESTED, itemId);
                    System.out.println(UIHelper.GREEN + "You liked '" + selected.getName() + "'." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "3" -> {
                    if (!requireLoggedInForAction("purchase an item")) {
                        continue;
                    }
                    purchase(scanner, broker);
                    UIHelper.pause();
                    return;
                }
                case "4" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    public static void viewWishlist(Scanner scanner, AsyncMessageBroker broker) {
        if (!requireLoggedInForAction("view wishlist"))
            return;

        List<?> wl = BrokerUtils.requestOnce(broker, EventType.WISHLIST_VIEW_REQUESTED, Main.currentUser.getId(),
                EventType.WISHLIST_DETAILS_RETURNED, 3000);

        if (wl == null || wl.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "Your wishlist is empty." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Get all items to map ids to details
        List<Item> items = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                EventType.ITEM_LIST_RETURNED, 3000);

        List<String> boxLines = new ArrayList<>();
        for (Object o : wl) {
            try {
                com.entities.Wishlist w = (com.entities.Wishlist) o;
                Item it = null;
                if (items != null)
                    it = items.stream().filter(x -> x.getId() == w.getItemId()).findFirst().orElse(null);
                if (it != null)
                    boxLines.add(String.format("#%d %s - $%.2f", it.getId(), it.getName(), it.getPrice()));
                else
                    boxLines.add(String.format("#%d (item data unavailable)", w.getItemId()));
            } catch (ClassCastException ex) {
                // ignore
            }
        }

        if (boxLines.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "Your wishlist is empty." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        UIHelper.box(UIHelper.color("YOUR WISHLIST", UIHelper.CYAN), boxLines);

        while (true) {
            System.out.println(UIHelper.YELLOW + "Actions:" + UIHelper.RESET);
            System.out.println("1. Purchase");
            System.out.println("2. Remove from Wishlist");
            System.out.println("3. Back to main menu");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    purchase(scanner, broker);
                    UIHelper.pause();
                    return;
                }
                case "2" -> {
                    System.out.print("Enter Item ID to remove from Wishlist: ");
                    String idInput = scanner.nextLine().trim();
                    Integer itemId = parseItemId(idInput);
                    if (itemId == null)
                        continue;

                    broker.publish(EventType.WISHLIST_REMOVE_REQUESTED,
                            new WishlistRemoveRequest(Main.currentUser.getId(), itemId));

                    System.out.println(UIHelper.GREEN + "Removed item from wishlist." + UIHelper.RESET);
                    UIHelper.pause();
                    return;
                }
                case "3" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    public static void viewOrders(Scanner scanner, AsyncMessageBroker broker) {
        viewOrders(scanner, broker, null);
    }

    private static void viewOrders(Scanner scanner, AsyncMessageBroker broker,
            com.repository.OrderRepository orderRepo) {
        if (!requireLoggedInForAction("view orders"))
            return;

        List<Order> orders = BrokerUtils.requestOnce(broker, EventType.ORDER_HISTORY_REQUESTED,
                Main.currentUser.getId(),
                EventType.ORDER_HISTORY_RETURNED, 3000);

        if (orders == null || orders.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "You have no orders." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        List<String> summaryLines = new ArrayList<>();
        for (Order order : orders) {
            summaryLines.add(String.format("#%d | Total: $%.2f | Status: %s",
                    order.getId(),
                    order.getTotalAmount(),
                    order.getStatus()));
        }

        UIHelper.box(UIHelper.color("YOUR ORDERS", UIHelper.CYAN), summaryLines);

        while (true) {
            System.out.println(UIHelper.YELLOW + "Actions:" + UIHelper.RESET);
            System.out.println("1. View order details");
            System.out.println("2. Back to Customer menu");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter Order ID to view details: ");
                    int orderId = scanner.nextInt();
                    scanner.nextLine();

                    Order order = orders.stream()
                            .filter(o -> o.getId() == orderId && o.getCustomerId() == Main.currentUser.getId())
                            .findFirst()
                            .orElse(null);

                    if (order == null) {
                        System.out.println(UIHelper.RED + "Order not found." + UIHelper.RESET);
                        continue;
                    }

                    showOrderDetails(order, broker, orderRepo);
                    return;
                }
                case "2" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    private static void showOrderDetails(Order order, AsyncMessageBroker broker,
            com.repository.OrderRepository orderRepo) {
        List<String> detailLines = new ArrayList<>();
        detailLines.add(String.format("Order ID: #%d", order.getId()));
        detailLines.add(String.format("Order Date: %s", formatOrderDate(order.getOrderDate())));
        detailLines.add(String.format("Status: %s", order.getStatus()));
        detailLines.add(String.format("Billing Address: %s", safeValue(order.getBillingAddress())));
        detailLines.add("");
        detailLines.add("Items:");

        boolean printedItems = false;
        // Try to reflectively obtain items if Order carries them; otherwise show a
        // helpful message
        try {
            java.lang.reflect.Method getItems = order.getClass().getMethod("getItems");
            Object itemsObj = getItems.invoke(order);
            if (itemsObj instanceof List<?> itemsList && !itemsList.isEmpty()) {
                for (Object obj : itemsList) {
                    String line;
                    try {
                        java.lang.reflect.Method gid = obj.getClass().getMethod("getItemId");
                        java.lang.reflect.Method gname = obj.getClass().getMethod("getItemName");
                        java.lang.reflect.Method gqty = obj.getClass().getMethod("getQuantity");
                        java.lang.reflect.Method gsub = obj.getClass().getMethod("getSubTotal");
                        Object iid = gid.invoke(obj);
                        Object nm = gname.invoke(obj);
                        Object q = gqty.invoke(obj);
                        Object st = gsub.invoke(obj);
                        line = String.format("  - #%s %s x%s -> $%s", iid, nm, q, st);
                    } catch (NoSuchMethodException nsme) {
                        line = "  - " + obj.toString();
                    }
                    detailLines.add(line);
                    printedItems = true;
                }
            }
        } catch (NoSuchMethodException name) {
        } catch (Exception e) {
        }

        if (!printedItems) {
            detailLines.add("(Item details not available)");
        }

        detailLines.add(String.format("Total Amount: $%.2f", order.getTotalAmount()));

        // Allow customer to change status from PLACED to DELIVERED
        if (Order.OrderStatus.PLACED.equals(order.getStatus())) {
            detailLines.add("");
            detailLines.add("[Order is PLACED - you can mark it as DELIVERED]");
        }

        UIHelper.box(UIHelper.color("ORDER DETAILS", UIHelper.GREEN), detailLines);

        // Allow status change after displaying details
        if (Order.OrderStatus.PLACED.equals(order.getStatus())) {
            System.out.println(UIHelper.YELLOW + "Mark order as DELIVERED? (y/n): " + UIHelper.RESET);
            Scanner statusScanner = new Scanner(System.in);
            String response = statusScanner.nextLine().trim().toLowerCase();
            if ("y".equals(response) || "yes".equals(response)) {
                order.setStatus(Order.OrderStatus.DELIVERED);
                // Persist status change to database
                if (orderRepo != null) {
                    orderRepo.updateStatus(order.getId(), Order.OrderStatus.DELIVERED);
                } else if (broker != null) {
                    broker.publish(EventType.ORDER_STATUS_UPDATE_REQUESTED, order);
                }
                System.out.println(UIHelper.GREEN + "Order marked as DELIVERED!" + UIHelper.RESET);
            }
        }

        UIHelper.pause();
    }

    private static String formatOrderDate(long epochMs) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        return ORDER_DATE_FORMAT.format(date);
    }

    public static void sendMessage(Scanner scanner, AsyncMessageBroker broker) {
        if (!requireLoggedInForAction("send messages"))
            return;

        System.out.print("Subject: ");
        String subject = scanner.nextLine();
        System.out.print("Your Message: ");
        String msg = scanner.nextLine();

        // Send to all staff (recipientId = -1 indicates broadcast to all staff)
        MessageSendRequest req = new MessageSendRequest(Main.currentUser.getId(), -1, subject, msg);
        broker.publish(EventType.MESSAGE_SEND_REQUESTED, req);

        System.out.println(UIHelper.GREEN + "Message sent." + UIHelper.RESET);
        UIHelper.pause();
    }

    private static void showNotificationMenu(Scanner scanner, AsyncMessageBroker broker) {
        System.out.println(UIHelper.CYAN + "=== Notifications ===" + UIHelper.RESET);

        // Get notifications from Main.notifications
        List<String> notifications = Main.getNotifications();

        if (notifications == null || notifications.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "(No notifications)" + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        for (String notification : notifications) {
            System.out.println(UIHelper.GREEN + "• " + notification + UIHelper.RESET);
        }

        System.out.println();
        System.out.println("Press 'c' to clear notifications, or Enter to continue...");
        String input = scanner.nextLine().trim().toLowerCase();
        if ("c".equals(input)) {
            Main.clearNotifications();
            System.out.println(UIHelper.YELLOW + "Notifications cleared." + UIHelper.RESET);
        }
        UIHelper.pause();
    }

    public static void viewAccount(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null) {
            System.out.println(UIHelper.RED + "No user is currently logged in." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Request authoritative account info from account subsystem
        User user = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_VIEW_REQUESTED, Main.currentUser.getId(),
                EventType.ACCOUNT_VIEW_RETURNED, 3000);

        if (user == null)
            user = Main.currentUser;

        List<String> info = List.of(
                "User ID: " + user.getId(),
                "Role: " + user.getRole(),
                "Username: " + safeValue(user.getUsername()),
                "Email: " + safeValue(user.getEmail()),
                "Phone: " + safeValue(user.getPhoneNumber()),
                "Address: " + safeValue(user.getAddress()));

        UIHelper.box(UIHelper.color("ACCOUNT INFORMATION", UIHelper.CYAN), info);

        // Display payment cards from database
        List<String> cardLines = new ArrayList<>();

        // Request payment cards for current user
        List<PaymentCard> cards = BrokerUtils.requestOnce(broker, EventType.PAYMENT_CARD_LIST_REQUESTED,
                Main.currentUser.getId(), EventType.PAYMENT_CARD_LIST_RETURNED, 3000);

        if (cards == null || cards.isEmpty()) {
            cardLines.add("(No payment cards available)");
            cardLines.add("Use add card to register one)");
        } else {
            for (PaymentCard card : cards) {
                cardLines.add(String.format("%s **** **** **** %s",
                        card.getCardType(),
                        card.getLast4Digits()));
                cardLines.add(String.format("Holder: %s | Expires: %s",
                        card.getCardHolderName(),
                        card.getExpiryDate()));
                cardLines.add("");
            }
        }

        UIHelper.box(UIHelper.color("PAYMENT CARDS", UIHelper.CYAN), cardLines);

        // Actions menu
        while (true) {
            System.out.println(UIHelper.YELLOW + "Actions:" + UIHelper.RESET);
            System.out.println("1. Edit Account");
            System.out.println("2. Back to main view");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);
            String act = scanner.nextLine().trim();

            switch (act) {
                case "1" -> editAccountMenu(scanner, broker, user);
                case "2" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    private static String maskCardNumber(String number) {
        if (number == null)
            return "N/A";
        String digits = number.replaceAll("\\s+", "");
        if (digits.length() <= 4)
            return digits;
        String last4 = digits.substring(digits.length() - 4);
        return "**** **** **** " + last4;
    }

    private static void editAccountMenu(Scanner scanner, AsyncMessageBroker broker, User currentDisplayUser) {
        while (true) {
            System.out.println(UIHelper.YELLOW + "Edit Account:" + UIHelper.RESET);
            System.out.println("1. Edit username");
            System.out.println("2. Edit phone");
            System.out.println("3. Edit Address");
            System.out.println("4. Edit password");
            System.out.println("5. Edit payment card");
            System.out.println("6. Add payment card");
            System.out.println("7. Back");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);
            String c = scanner.nextLine().trim();

            switch (c) {
                case "1" -> {
                    System.out.print("New username: ");
                    String v = scanner.nextLine().trim();
                    broker.publish(EventType.ACCOUNT_EDIT_REQUESTED, new com.common.dto.account.AccountEditRequest(
                            currentDisplayUser.getId(), v, null, null, null, null));
                    // Wait for update success
                    User updated = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_EDIT_REQUESTED, null,
                            EventType.ACCOUNT_UPDATE_SUCCESS, 3000);
                    if (updated != null) {
                        System.out.println(UIHelper.GREEN + "Username updated successfully!" + UIHelper.RESET);
                        UIHelper.pause();
                        // Refresh account display
                        viewAccount(scanner, broker);
                        return;
                    } else {
                        System.out.println(UIHelper.RED + "Username update failed." + UIHelper.RESET);
                    }
                }
                case "2" -> {
                    System.out.print("New phone: ");
                    String v = scanner.nextLine().trim();
                    broker.publish(EventType.ACCOUNT_EDIT_REQUESTED, new com.common.dto.account.AccountEditRequest(
                            currentDisplayUser.getId(), null, null, v, null, null));
                    // Wait for update success
                    User updated = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_EDIT_REQUESTED, null,
                            EventType.ACCOUNT_UPDATE_SUCCESS, 3000);
                    if (updated != null) {
                        System.out.println(UIHelper.GREEN + "Phone updated successfully!" + UIHelper.RESET);
                        UIHelper.pause();
                        // Refresh account display
                        viewAccount(scanner, broker);
                        return;
                    } else {
                        System.out.println(UIHelper.RED + "Phone update failed." + UIHelper.RESET);
                    }
                }
                case "3" -> {
                    System.out.print("New address: ");
                    String v = scanner.nextLine().trim();
                    broker.publish(EventType.ACCOUNT_EDIT_REQUESTED, new com.common.dto.account.AccountEditRequest(
                            currentDisplayUser.getId(), null, null, null, v, null));
                    // Wait for update success
                    User updated = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_EDIT_REQUESTED, null,
                            EventType.ACCOUNT_UPDATE_SUCCESS, 3000);
                    if (updated != null) {
                        System.out.println(UIHelper.GREEN + "Address updated successfully!" + UIHelper.RESET);
                        UIHelper.pause();
                        // Refresh account display
                        viewAccount(scanner, broker);
                        return;
                    } else {
                        System.out.println(UIHelper.RED + "Address update failed." + UIHelper.RESET);
                    }
                }
                case "4" -> {
                    // Edit password: verify current password first
                    System.out.print("Current password: ");
                    String currentPw = Main.readPasswordHidden();

                    // Publish login request to verify password
                    broker.publish(EventType.USER_LOGIN_REQUEST,
                            new com.common.dto.auth.LoginRequest(currentDisplayUser.getUsername(), currentPw));
                    // Wait for login response
                    Object loginResp = BrokerUtils.requestOnce(broker, EventType.USER_LOGIN_REQUEST, null,
                            EventType.USER_LOGIN_SUCCESS, 2000);

                    if (loginResp != null) {
                        // Password is correct, ask for new password
                        System.out.print("New password: ");
                        String newPw = Main.readPasswordHidden();
                        System.out.print("Confirm new password: ");
                        String confirmPw = Main.readPasswordHidden();

                        if (!newPw.equals(confirmPw)) {
                            System.out.println(UIHelper.RED + "Passwords do not match." + UIHelper.RESET);
                            continue;
                        }

                        // Publish password change request
                        broker.publish(EventType.ACCOUNT_EDIT_REQUESTED, new com.common.dto.account.AccountEditRequest(
                                currentDisplayUser.getId(), null, null, null, null, newPw));
                        User updated = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_EDIT_REQUESTED, null,
                                EventType.ACCOUNT_UPDATE_SUCCESS, 3000);

                        if (updated != null) {
                            System.out.println(UIHelper.GREEN + "Password changed successfully!" + UIHelper.RESET);
                            UIHelper.pause();
                            viewAccount(scanner, broker);
                            return;
                        } else {
                            System.out.println(UIHelper.RED + "Password change failed." + UIHelper.RESET);
                        }
                    } else {
                        System.out.println(UIHelper.RED + "Current password is incorrect." + UIHelper.RESET);
                    }
                }
                case "5" -> {
                    System.out.println(
                            UIHelper.YELLOW + "Payment card editing is not fully implemented." + UIHelper.RESET);
                    System.out.println(
                            UIHelper.YELLOW + "Please use 'Add payment card' to add new cards." + UIHelper.RESET);
                }
                case "6" -> {
                    System.out.print("Card number: ");
                    String cardNum = scanner.nextLine().trim();
                    System.out.print("Cardholder name: ");
                    String cardName = scanner.nextLine().trim();
                    System.out.print("Expiry (MM/YY): ");
                    String expiry = scanner.nextLine().trim();

                    // Create and publish payment card add request
                    com.common.dto.payment.PaymentCardAddRequest cardRequest = new com.common.dto.payment.PaymentCardAddRequest(
                            currentDisplayUser.getId(), cardName, cardNum, expiry, "CREDIT");
                    broker.publish(EventType.PAYMENT_CARD_ADD_REQUESTED, cardRequest);
                    System.out.println(UIHelper.GREEN + "Payment card added successfully!" + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "7" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    private static String safeValue(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

}
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
import com.common.dto.wishlist.WishlistViewRequest;
import com.entities.Item;
import com.entities.Order;
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
                        "6. View Conversations",
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
            case "6" -> viewConversations(broker);
            case "7" -> viewAccount(broker);
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
                    System.out.print("Enter ItemID to add to Wishlist: ");
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

                    broker.publish(EventType.ITEM_LIKE_REQUESTED, itemId);
                    System.out.println(UIHelper.GREEN + "You liked '" + selected.getName() + "'." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "3" -> {
                    if (!requireLoggedInForAction("purchase an item")) {
                        continue;
                    }
                    boolean completed = purchase(scanner, broker);
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
        System.out.print("Enter ItemID to purchase: ");
        int itemId = Integer.parseInt(scanner.nextLine());

        // Fetch item info
        List<Item> items = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                EventType.ITEM_LIST_RETURNED, 3000);
        Item item = null;
        if (items != null)
            item = items.stream().filter(i -> i.getId() == itemId).findFirst().orElse(null);

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
                    System.out.print("Enter ItemID to add to Wishlist: ");
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
                    System.out.print("Enter ItemID to like: ");
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
                    boolean completed = purchase(scanner, broker);
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

        List<?> wl = BrokerUtils.requestOnce(broker, EventType.WISHLIST_VIEW_REQUESTED, 
                new WishlistViewRequest(Main.currentUser.getId()),
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
                    boolean completed = purchase(scanner, broker);
                    UIHelper.pause();
                    return;
                }
                case "2" -> {
                    System.out.print("Enter ItemID to remove from Wishlist: ");
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
                    System.out.print("Enter OrderID to view details: ");
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

                    showOrderDetails(order);
                    return;
                }
                case "2" -> {
                    return;
                }
                default -> System.out.println(UIHelper.RED + "Invalid Input" + UIHelper.RESET);
            }
        }
    }

    private static void showOrderDetails(Order order) {
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
        } catch (NoSuchMethodException nsme) {
            // no getItems() - ignore
        } catch (Exception e) {
            // reflection failed; fall back
        }

        if (!printedItems) {
            detailLines.add("  (Item details not available)");
        }

        detailLines.add(String.format("Total Amount: $%.2f", order.getTotalAmount()));

        UIHelper.box(UIHelper.color("ORDER DETAILS", UIHelper.GREEN), detailLines);
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
        System.out.print("Message: ");
        String msg = scanner.nextLine();

        // Send to all staff (recipientId = -1 indicates broadcast to all staff)
        MessageSendRequest req = new MessageSendRequest(Main.currentUser.getId(), -1, subject, msg);
        broker.publish(EventType.MESSAGE_SEND_REQUESTED, req);

        System.out.println(UIHelper.GREEN + "Message sent." + UIHelper.RESET);
        UIHelper.pause();
    }

    private static void viewConversations(AsyncMessageBroker broker) {
        // Request conversation list and render it locally to avoid duplicate prints
        // from Main
        List<com.entities.Conversation> convs = BrokerUtils.requestOnce(
                broker,
                EventType.CONVERSATION_LIST_REQUESTED,
                new ConversationListRequest(Main.currentUser.getId()),
                EventType.CONVERSATION_LIST_RETURNED,
                3000);

        if (convs == null || convs.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No conversations found." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        System.out.println("=== Conversations ===");
        for (com.entities.Conversation conv : convs) {
            if (Main.currentUser.getRole() == User.Role.CUSTOMER) {
                System.out.println("With: " + conv.getStaffName()
                        + " | Last: " + conv.getLastMessage()
                        + " | Unread: " + conv.getUnreadCount());
            } else {
                System.out.println("Customer ID: " + conv.getCustomerId()
                        + " (" + conv.getCustomerName() + ")"
                        + " | Last: " + conv.getLastMessage()
                        + " | Unread: " + conv.getUnreadCount());
            }
        }
    }

    public static void viewAccount(AsyncMessageBroker broker) {
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
        UIHelper.pause();
    }

    private static String safeValue(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

}
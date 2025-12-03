package com.ui;

import com.Main;
import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.common.dto.account.AccountViewRequest;
import com.common.dto.item.ItemEditRequest;
import com.common.dto.item.ItemUploadRequest;
import com.common.dto.message.ConversationLoadRequest;
import com.common.dto.message.MessageSendRequest;
import com.common.dto.message.UnreadMessagesRequest;
import com.entities.Item;
import com.entities.Order;
import com.entities.User;
import com.entities.UserMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class StaffUI {

    public static void showMenu(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.clear();

        UIHelper.box(
                UIHelper.color("STAFF MENU", UIHelper.GREEN),
                List.of(
                        "1. Browse Items",
                        "2. Refill Inventory",
                        "3. Upload Item",
                        "4. Edit Item",
                        "5. Reply to Customer",
                        "6. Notifications",
                        "7. View Customer Orders",
                        "8. Logout",
                        "9. Quit"));

        System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
        String input = scanner.nextLine();

        switch (input) {
            case "1" -> browse(scanner, broker);
            case "2" -> refill(scanner, broker);
            case "3" -> upload(scanner, broker);
            case "4" -> edit(scanner, broker);
            case "5" -> replyToCustomer(scanner, broker);
            case "6" -> viewNotifications(scanner, broker);
            case "7" -> viewCustomerOrders(scanner, broker);
            case "8" -> Main.currentUser = null;
            case "9" -> System.exit(0);
            case "Q", "q" -> System.exit(0);
            default -> {
                System.out.println(UIHelper.RED + "Invalid option!" + UIHelper.RESET);
                UIHelper.pause();
            }
        }
    }

    // =========================
    // VIEW CUSTOMER ORDERS
    // =========================
    private static void viewCustomerOrders(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null || Main.currentUser.getRole() != User.Role.STAFF) {
            System.out.println(UIHelper.RED + "Only staff can view customer orders." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Step 1: load all customers that have orders
        List<Integer> customerIds = BrokerUtils.requestOnce(
                broker,
                EventType.ORDER_CUSTOMER_LIST_REQUESTED,
                null,
                EventType.ORDER_CUSTOMER_LIST_RETURNED,
                3000);

        if (customerIds == null || customerIds.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No customers have placed orders yet." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        List<User> customers = new ArrayList<>();
        System.out.println(UIHelper.CYAN + "=== Customers with Orders ===" + UIHelper.RESET);
        int displayIndex = 1;
        for (Integer cid : customerIds) {
            User u = BrokerUtils.requestOnce(
                    broker,
                    EventType.ACCOUNT_VIEW_REQUESTED,
                    new AccountViewRequest(cid),
                    EventType.ACCOUNT_VIEW_RETURNED,
                    2000);
            customers.add(u);
            String username = (u != null) ? safe(u.getUsername()) : "(unknown)";
            System.out.printf("%d) %s (Customer ID: %d)%n", displayIndex, username, cid);
            displayIndex++;
        }
        System.out.println("0) Back");

        System.out.print(UIHelper.YELLOW
                + "Select a customer (enter list number or customer ID, 0 to cancel): " + UIHelper.RESET);
        String sel = scanner.nextLine().trim();
        if (sel.isEmpty() || "0".equals(sel)) {
            return;
        }

        Integer customerId = null;
        try {
            int numeric = Integer.parseInt(sel);
            if (numeric >= 1 && numeric <= customerIds.size()) {
                customerId = customerIds.get(numeric - 1);
            } else if (customerIds.contains(numeric)) {
                customerId = numeric;
            } else {
                System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
                UIHelper.pause();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Find loaded customer object (may be null if not found)
        User customer = null;
        for (User u : customers) {
            if (u != null && u.getId() == customerId) {
                customer = u;
                break;
            }
        }

        List<Order> orders = BrokerUtils.requestOnce(
                broker,
                EventType.ORDER_HISTORY_REQUESTED,
                customerId,
                EventType.ORDER_HISTORY_RETURNED,
                3000);

        if (orders == null || orders.isEmpty()) {
            String name = customer != null ? safe(customer.getUsername()) : ("ID " + customerId);
            System.out.println(UIHelper.YELLOW + "Customer " + name + " has no orders." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        while (true) {
            UIHelper.clear();
            String headerName = customer != null ? safe(customer.getUsername()) : ("ID " + customerId);
            System.out.println(UIHelper.CYAN + "=== Orders of Customer " + headerName + " (ID " + customerId + ") ==="
                    + UIHelper.RESET);
            for (Order o : orders) {
                System.out.printf("Order #%d | Total: $%.2f | Status: %s%n",
                        o.getId(), o.getTotalAmount(), o.getStatus());
            }
            System.out.println("\nActions:");
            System.out.println("1. View order details");
            System.out.println("2. Change order status");
            System.out.println("3. Back");
            System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    Integer orderId = readPositiveInt(scanner, "Enter Order ID to view details: ");
                    if (orderId == null)
                        break;
                    Order target = null;
                    for (Order o : orders) {
                        if (o.getId() == orderId) {
                            target = o;
                            break;
                        }
                    }
                    if (target == null) {
                        System.out.println(UIHelper.RED + "Order not found for this customer." + UIHelper.RESET);
                        UIHelper.pause();
                        break;
                    }
                    showOrderDetailsForStaff(scanner, broker, target);
                }
                case "2" -> {
                    Integer orderId = readPositiveInt(scanner, "Enter Order ID to change status: ");
                    if (orderId == null)
                        break;
                    Order target = null;
                    for (Order o : orders) {
                        if (o.getId() == orderId) {
                            target = o;
                            break;
                        }
                    }
                    if (target == null) {
                        System.out.println(UIHelper.RED + "Order not found for this customer." + UIHelper.RESET);
                        UIHelper.pause();
                        break;
                    }
                    changeOrderStatus(scanner, broker, target);
                    // reload orders after status change
                    orders = BrokerUtils.requestOnce(
                            broker,
                            EventType.ORDER_HISTORY_REQUESTED,
                            customerId,
                            EventType.ORDER_HISTORY_RETURNED,
                            3000);
                }
                case "3" -> {
                    return;
                }
                default -> {
                    System.out.println(UIHelper.RED + "Invalid option." + UIHelper.RESET);
                    UIHelper.pause();
                }
            }
        }
    }

    public static void browse(Scanner scanner, AsyncMessageBroker broker) {
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
        UIHelper.pause();
    }

    public static void refill(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.box(
                UIHelper.color("REFILL INVENTORY", UIHelper.GREEN),
                List.of(
                        "Provide the item ID and quantity to restock."));

        Integer id = readPositiveInt(scanner, "Item ID: ");
        if (id == null)
            return;

        Integer qty = readPositiveInt(scanner, "Add Quantity: ");
        if (qty == null)
            return;

        broker.publish(EventType.ITEM_REFILL_REQUESTED,
                new ItemEditRequest(id, null, null, null, qty, null));

        UIHelper.loading("Updating inventory");
        System.out.println(UIHelper.GREEN + "Inventory updated successfully!" + UIHelper.RESET);
        UIHelper.pause();
    }

    public static void upload(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.box(
                UIHelper.color("UPLOAD NEW ITEM", UIHelper.GREEN),
                List.of("Fill out the new item details below."));

        System.out.print("Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println(UIHelper.RED + "Name cannot be empty." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        System.out.println("Description:");
        String description = scanner.nextLine().trim();

        System.out.print("Category (optional): ");
        String category = scanner.nextLine().trim();

        Double price = readPositiveDouble(scanner, "Price: ");
        if (price == null)
            return;

        Integer stock = readPositiveInt(scanner, "Stock: ");
        if (stock == null)
            return;

        // Use 5-arg constructor to pass category (may be empty)
        broker.publish(EventType.ITEM_UPLOAD_REQUESTED,
                new ItemUploadRequest(name, description, price, stock,
                        category.isEmpty() ? null : category));

        UIHelper.loading("Uploading item");
        System.out.println(UIHelper.GREEN + "Item uploaded!" + UIHelper.RESET);
        UIHelper.pause();
    }

    public static void edit(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.box(
                UIHelper.color("EDIT ITEM", UIHelper.GREEN),
                List.of("Select an item and choose which field to edit."));

        Integer id = readPositiveInt(scanner, "Item ID: ");
        if (id == null)
            return;

        // Optional: confirm item exists (load list and search)
        List<Item> items = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                EventType.ITEM_LIST_RETURNED, 3000);
        Item target = null;
        if (items != null) {
            for (Item i : items) {
                if (i.getId() == id) {
                    target = i;
                    break;
                }
            }
        }
        if (target == null) {
            System.out.println(UIHelper.RED + "Item with ID " + id + " not found." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        while (true) {
            System.out.println(UIHelper.YELLOW + "Edit Options for Item #" + id + ":" + UIHelper.RESET);
            System.out.println("1. Edit name");
            System.out.println("2. Edit description");
            System.out.println("3. Edit category");
            System.out.println("4. Edit price");
            System.out.println("5. Back");
            System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("New Name: ");
                    String name = scanner.nextLine().trim();
                    if (name.isEmpty()) {
                        System.out.println(UIHelper.RED + "Name cannot be empty." + UIHelper.RESET);
                        break;
                    }
                    broker.publish(EventType.ITEM_EDIT_REQUESTED,
                            new ItemEditRequest(id, name, null, null, null, null));
                    UIHelper.loading("Saving changes");
                    System.out.println(UIHelper.GREEN + "Item name updated." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "2" -> {
                    System.out.print("New Description: ");
                    String desc = scanner.nextLine().trim();
                    broker.publish(EventType.ITEM_EDIT_REQUESTED,
                            new ItemEditRequest(id, null, desc, null, null, null));
                    UIHelper.loading("Saving changes");
                    System.out.println(UIHelper.GREEN + "Item description updated." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "3" -> {
                    System.out.print("New Category: ");
                    String category = scanner.nextLine().trim();
                    broker.publish(EventType.ITEM_EDIT_REQUESTED,
                            new ItemEditRequest(id, null, null, null, null,
                                    category.isEmpty() ? null : category));
                    UIHelper.loading("Saving changes");
                    System.out.println(UIHelper.GREEN + "Item category updated." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "4" -> {
                    Double price = readPositiveDouble(scanner, "New Price: ");
                    if (price == null)
                        break;
                    broker.publish(EventType.ITEM_EDIT_REQUESTED,
                            new ItemEditRequest(id, null, null, price, null, null));
                    UIHelper.loading("Saving changes");
                    System.out.println(UIHelper.GREEN + "Item price updated." + UIHelper.RESET);
                    UIHelper.pause();
                }
                case "5" -> {
                    return;
                }
                default -> {
                    System.out.println(UIHelper.RED + "Invalid option!" + UIHelper.RESET);
                    UIHelper.pause();
                }
            }
        }
    }

    // =========================
    // STAFF NOTIFICATIONS
    // =========================
    private static void viewNotifications(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null || Main.currentUser.getRole() != User.Role.STAFF) {
            System.out.println(UIHelper.RED + "Notifications are only available for staff." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        List<UserMessage> unread = BrokerUtils.requestOnce(
                broker,
                EventType.UNREAD_MESSAGES_REQUESTED,
                new UnreadMessagesRequest(Main.currentUser.getId()),
                EventType.UNREAD_MESSAGES_RETURNED,
                3000);

        if (unread == null || unread.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No unread messages from customers." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        System.out.println(UIHelper.CYAN + "=== Customer Message Notifications ===" + UIHelper.RESET);
        for (int i = 0; i < unread.size(); i++) {
            UserMessage m = unread.get(i);
            int displayIndex = i + 1;
            System.out.printf("%d) From Customer ID: %d | Subject: %s%n",
                    displayIndex, m.getSenderId(), m.getSubject());
        }
        System.out.println("0) Back");

        System.out.print(UIHelper.YELLOW + "Select a notification to open chat: " + UIHelper.RESET);
        String sel = scanner.nextLine().trim();
        int idx;
        try {
            idx = Integer.parseInt(sel);
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }
        if (idx <= 0 || idx > unread.size()) {
            return;
        }

        UserMessage chosen = unread.get(idx - 1);
        int customerId = chosen.getSenderId();
        openChatWithCustomer(scanner, broker, customerId);
    }

    private static void replyToCustomer(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null || Main.currentUser.getRole() != User.Role.STAFF) {
            System.out.println(UIHelper.RED + "Only staff can reply to customers." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Load all conversations for this staff to list active customers
        List<com.entities.Conversation> conversations = BrokerUtils.requestOnce(
                broker,
                EventType.CONVERSATION_LIST_REQUESTED,
                new com.common.dto.message.ConversationListRequest(Main.currentUser.getId()),
                EventType.CONVERSATION_LIST_RETURNED,
                3000);

        if (conversations == null || conversations.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No active customer conversations." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        System.out.println(UIHelper.CYAN + "=== Customers with Conversations ===" + UIHelper.RESET);
        Set<Integer> uniqueCustomerIds = new LinkedHashSet<>();
        List<Integer> displayOrder = new ArrayList<>();
        int index = 1;
        for (com.entities.Conversation conv : conversations) {
            int customerId = conv.getCustomerId();
            if (!uniqueCustomerIds.add(customerId)) {
                continue;
            }
            displayOrder.add(customerId);

            // Fetch basic customer info
            User customer = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_VIEW_REQUESTED,
                    new AccountViewRequest(customerId),
                    EventType.ACCOUNT_VIEW_RETURNED,
                    2000);

            System.out.printf("%d) Customer ID: %d%n", index, customerId);
            if (customer != null) {
                System.out.println("   Username: " + safe(customer.getUsername()));
                System.out.println("   Email:    " + safe(customer.getEmail()));
                System.out.println("   Phone:    " + safe(customer.getPhoneNumber()));
                System.out.println("   Address:  " + safe(customer.getAddress()));
            }
            System.out.println("   Last Msg: " + safe(conv.getLastMessage()));
            System.out.println("   Unread:   " + conv.getUnreadCount());
            System.out.println("---");
            index++;
        }

        if (displayOrder.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "No customers available." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        System.out.print(UIHelper.YELLOW + "Select a customer (enter list number or customer ID, 0 to cancel): " + UIHelper.RESET);
        String sel = scanner.nextLine().trim();
        if (sel.isEmpty() || "0".equals(sel)) {
            return;
        }

        Integer selectedCustomerId = null;
        try {
            int numeric = Integer.parseInt(sel);
            if (numeric >= 1 && numeric <= displayOrder.size()) {
                selectedCustomerId = displayOrder.get(numeric - 1);
            } else if (uniqueCustomerIds.contains(numeric)) {
                selectedCustomerId = numeric;
            } else {
                System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
                UIHelper.pause();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        openChatWithCustomer(scanner, broker, selectedCustomerId);
    }

    public static void reply(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.box(
                UIHelper.color("REPLY TO MESSAGES", UIHelper.GREEN),
                List.of("Load customer messages to respond."));
        UIHelper.loading("Retrieving messages");
        broker.publish(EventType.MESSAGE_REPLY_REQUESTED, null);
        UIHelper.pause();
    }

    // =========================
    // CHAT BOX WITH CUSTOMER
    // =========================
    private static void openChatWithCustomer(Scanner scanner, AsyncMessageBroker broker, int customerId) {
        User customer = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_VIEW_REQUESTED,
                new AccountViewRequest(customerId),
                EventType.ACCOUNT_VIEW_RETURNED,
                3000);

        List<Order> orders = BrokerUtils.requestOnce(broker,
                EventType.ORDER_HISTORY_REQUESTED,
                customerId,
                EventType.ORDER_HISTORY_RETURNED,
                3000);

        while (true) {
            UIHelper.clear();
            System.out.println(UIHelper.CYAN + "=== CUSTOMER CHAT ===" + UIHelper.RESET);
            System.out.println("Customer ID: " + customerId);
            if (customer != null) {
                System.out.println("Username: " + safe(customer.getUsername()));
                System.out.println("Email:    " + safe(customer.getEmail()));
                System.out.println("Phone:    " + safe(customer.getPhoneNumber()));
                System.out.println("Address:  " + safe(customer.getAddress()));
            }

            if (orders != null && !orders.isEmpty()) {
                System.out.println("\nRecent Orders:");
                int shown = 0;
                for (Order o : orders) {
                    System.out.printf(" - Order #%d | Total: $%.2f | Status: %s%n",
                            o.getId(), o.getTotalAmount(), o.getStatus());
                    shown++;
                    if (shown >= 3)
                        break;
                }
            }

            List<UserMessage> history = BrokerUtils.requestOnce(
                    broker,
                    EventType.CONVERSATION_LOAD_REQUESTED,
                    new ConversationLoadRequest(Main.currentUser.getId(), customerId),
                    EventType.CONVERSATION_MESSAGES_RETURNED,
                    3000);

            System.out.println(UIHelper.CYAN + "\n--- Conversation History ---" + UIHelper.RESET);
            if (history == null || history.isEmpty()) {
                System.out.println(UIHelper.YELLOW + "(No previous messages)" + UIHelper.RESET);
            } else {
                for (UserMessage m : history) {
                    String sender = (m.getSenderId() == Main.currentUser.getId())
                            ? "Me"
                            : (customer != null ? safe(customer.getUsername()) : "Customer");
                    System.out.println("[" + sender + "]: " + m.getContent());
                }
            }
            System.out.println(UIHelper.CYAN + "------------------------------" + UIHelper.RESET);

            System.out.println("\nActions:");
            System.out.println("1. Send message");
            System.out.println("2. Back");
            System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
            String action = scanner.nextLine().trim();

            if ("1".equals(action)) {
                System.out.print("Your reply (leave blank to cancel): ");
                String reply = scanner.nextLine().trim();
                if (reply.isEmpty()) {
                    System.out.println(UIHelper.YELLOW + "Reply canceled." + UIHelper.RESET);
                    UIHelper.pause();
                    continue;
                }
                broker.publish(EventType.MESSAGE_SEND_REQUESTED,
                        new MessageSendRequest(Main.currentUser.getId(), customerId, "Reply", reply));
                System.out.println(UIHelper.GREEN + "Reply sent successfully." + UIHelper.RESET);
                UIHelper.pause();
            } else if ("2".equals(action)) {
                return;
            } else {
                System.out.println(UIHelper.RED + "Invalid option." + UIHelper.RESET);
                UIHelper.pause();
            }
        }
    }

    // =========================
    // ORDER DETAILS & STATUS CHANGE
    // =========================
    private static void showOrderDetailsForStaff(Scanner scanner, AsyncMessageBroker broker, Order order) {
        List<String> detailLines = new ArrayList<>();
        detailLines.add(String.format("Order ID: #%d", order.getId()));
        detailLines.add(String.format("Customer ID: %d", order.getCustomerId()));
        detailLines.add(String.format("Order Date: %d", order.getOrderDate()));
        detailLines.add(String.format("Status: %s", order.getStatus()));
        detailLines.add(String.format("Total Amount: $%.2f", order.getTotalAmount()));
        detailLines.add(String.format("Billing Address: %s",
                order.getBillingAddress() == null || order.getBillingAddress().isBlank()
                        ? "N/A"
                        : order.getBillingAddress()));

        UIHelper.box(UIHelper.color("ORDER DETAILS", UIHelper.CYAN), detailLines);
        UIHelper.pause();
    }

    private static void changeOrderStatus(Scanner scanner, AsyncMessageBroker broker, Order order) {
        System.out.println(UIHelper.CYAN + "Current status: " + order.getStatus() + UIHelper.RESET);
        System.out.println("Select new status:");
        System.out.println("1. PLACED");
        System.out.println("2. SHIPPED");
        System.out.println("3. DELIVERED");
        System.out.println("4. CANCELED");
        System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
        String choice = scanner.nextLine().trim();

        Order.OrderStatus newStatus = null;
        switch (choice) {
            case "1" -> newStatus = Order.OrderStatus.PLACED;
            case "2" -> newStatus = Order.OrderStatus.SHIPPED;
            case "3" -> newStatus = Order.OrderStatus.DELIVERED;
            case "4" -> newStatus = Order.OrderStatus.CANCELED;
            default -> {
                System.out.println(UIHelper.RED + "Invalid status selection." + UIHelper.RESET);
                UIHelper.pause();
                return;
            }
        }

        if (newStatus == order.getStatus()) {
            System.out.println(UIHelper.YELLOW + "Order is already in this status." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Update in-memory object then ask OrderManagement to persist
        order.setStatus(newStatus);
        broker.publish(EventType.ORDER_STATUS_UPDATE_REQUESTED, order);
        System.out.println(UIHelper.GREEN + "Order status update requested: " + newStatus + UIHelper.RESET);
        UIHelper.pause();
    }

    private static String safe(String v) {
        return v == null || v.isBlank() ? "N/A" : v;
    }

    private static Integer readPositiveInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0)
                throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid number." + UIHelper.RESET);
            UIHelper.pause();
            return null;
        }
    }

    private static Double readPositiveDouble(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();
        try {
            double parsed = Double.parseDouble(value);
            if (parsed <= 0)
                throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid amount." + UIHelper.RESET);
            UIHelper.pause();
            return null;
        }
    }
}
package com.ui;

import com.Main;
import com.broker.AsyncMessageBroker;
import com.broker.EventType;
import com.common.dto.account.AccountViewRequest;
import com.common.dto.item.*;
import com.common.dto.message.ConversationListRequest;
import com.common.dto.message.MessageSendRequest;
import com.common.dto.message.UnreadMessagesRequest;
import com.entities.Item;
import com.entities.User;

import java.util.List;
import java.util.Scanner;

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
                        "5. View Unread Messages",
                        "6. View All Conversations",
                        "7. Reply to Customer",
                        "8. Logout",
                        "Q. Quit"));

        System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
        String input = scanner.nextLine();

        switch (input) {
            case "1" -> browse(scanner, broker);
            case "2" -> refill(scanner, broker);
            case "3" -> upload(scanner, broker);
            case "4" -> edit(scanner, broker);
            case "5" -> viewUnreadMessages(broker);
            case "6" -> viewAllConversations(broker);
            case "7" -> replyToCustomer(scanner, broker);
            case "8" -> Main.currentUser = null;
            case "Q", "q" -> System.exit(0);
            default -> {
                System.out.println(UIHelper.RED + "Invalid option!" + UIHelper.RESET);
                UIHelper.pause();
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

        Double price = readPositiveDouble(scanner, "Price: ");
        if (price == null)
            return;

        Integer stock = readPositiveInt(scanner, "Stock: ");
        if (stock == null)
            return;

        broker.publish(EventType.ITEM_UPLOAD_REQUESTED,
                new ItemUploadRequest(name, description, price, stock));

        UIHelper.loading("Uploading item");
        System.out.println(UIHelper.GREEN + "Item uploaded!" + UIHelper.RESET);
        UIHelper.pause();
    }

    public static void edit(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.box(
                UIHelper.color("EDIT ITEM", UIHelper.GREEN),
                List.of("Update the item details below."));

        Integer id = readPositiveInt(scanner, "Item ID: ");
        if (id == null) return;

        System.out.print("New Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("New Description: ");
        String desc = scanner.nextLine().trim();

        Double price = readPositiveDouble(scanner, "New Price: ");
        if (price == null) return;

        Integer stock = readPositiveInt(scanner, "New Stock: ");
        if (stock == null) return;

        broker.publish(EventType.ITEM_EDIT_REQUESTED,
                new ItemEditRequest(id, name, desc, price, stock, null));

        UIHelper.loading("Saving changes");
        System.out.println(UIHelper.GREEN + "Item updated." + UIHelper.RESET);
        UIHelper.pause();
    }

    private static void viewUnreadMessages(AsyncMessageBroker broker) {
        broker.publish(EventType.UNREAD_MESSAGES_REQUESTED,
                new UnreadMessagesRequest(Main.currentUser.getId()));
    }

    private static void viewAllConversations(AsyncMessageBroker broker) {
        broker.publish(EventType.CONVERSATION_LIST_REQUESTED,
                new ConversationListRequest(Main.currentUser.getId()));
    }

    private static void replyToCustomer(Scanner scanner, AsyncMessageBroker broker) {
        Integer customerId = readPositiveInt(scanner, "Customer ID to reply to: ");
        if (customerId == null) {
            System.out.println(UIHelper.RED + "Invalid customer ID." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Fetch customer information
        System.out.println(UIHelper.YELLOW + "Fetching customer information..." + UIHelper.RESET);
        User customer = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_VIEW_REQUESTED, 
                new AccountViewRequest(customerId),
                EventType.ACCOUNT_VIEW_RETURNED, 3000);

        if (customer == null) {
            System.out.println(UIHelper.RED + "Customer with ID " + customerId + " not found." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Display customer information
        System.out.println(UIHelper.CYAN + "\n=== Customer Information ===" + UIHelper.RESET);
        System.out.println("Customer ID: " + customer.getId());
        System.out.println("Username: " + (customer.getUsername() != null ? customer.getUsername() : "N/A"));
        System.out.println("Email: " + (customer.getEmail() != null ? customer.getEmail() : "N/A"));
        System.out.println("Phone: " + (customer.getPhoneNumber() != null ? customer.getPhoneNumber() : "N/A"));
        System.out.println("Role: " + customer.getRole());
        System.out.println(UIHelper.CYAN + "============================\n" + UIHelper.RESET);

        // Fetch and display recent messages from this customer
        broker.publish(EventType.CONVERSATION_LOAD_REQUESTED, 
                new com.common.dto.message.ConversationLoadRequest(Main.currentUser.getId(), customerId));
        
        // Wait a moment for messages to be displayed
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.print("\nYour reply message: ");
        String reply = scanner.nextLine().trim();

        if (reply.isEmpty()) {
            System.out.println(UIHelper.RED + "Reply message cannot be empty." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Staff replying directly to customer (not broadcast)
        broker.publish(EventType.MESSAGE_SEND_REQUESTED,
                new MessageSendRequest(Main.currentUser.getId(), customerId, "Reply", reply));

        System.out.println(UIHelper.GREEN + "Reply sent successfully to " + customer.getUsername() + "." + UIHelper.RESET);
        UIHelper.pause();
    }

    public static void reply(Scanner scanner, AsyncMessageBroker broker) {
        UIHelper.box(
                UIHelper.color("REPLY TO MESSAGES", UIHelper.GREEN),
                List.of("Load customer messages to respond."));
        UIHelper.loading("Retrieving messages");
        broker.publish(EventType.MESSAGE_REPLY_REQUESTED, null);
        UIHelper.pause();
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
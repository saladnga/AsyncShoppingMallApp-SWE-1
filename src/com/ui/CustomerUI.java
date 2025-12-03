package com.ui;

import com.Main;
import com.broker.AsyncMessageBroker;
import com.broker.EventType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

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

    // ======================================================
    // WISHLIST PURCHASE FLOW
    // ======================================================
    private static boolean handleWishlistPurchase(Scanner scanner, AsyncMessageBroker broker,
            List<com.entities.Wishlist> wishlistEntries, List<Item> catalog) {
        if (wishlistEntries == null || wishlistEntries.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "Your wishlist is empty." + UIHelper.RESET);
            return false;
        }

        if (catalog == null || catalog.isEmpty()) {
            catalog = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                    EventType.ITEM_LIST_RETURNED, 3000);
        }

        if (catalog == null || catalog.isEmpty()) {
            System.out.println(UIHelper.RED + "Unable to load item catalog. Please try again later." + UIHelper.RESET);
            return false;
        }

        Map<Integer, Item> itemMap = new HashMap<>();
        for (Item item : catalog) {
            itemMap.put(item.getId(), item);
        }

        while (true) {
            System.out.println(UIHelper.YELLOW + "Purchase wishlist items:" + UIHelper.RESET);
            System.out.println("1. Purchase all items");
            System.out.println("2. Select specific items");
            System.out.println("3. Cancel");
            System.out.print(UIHelper.YELLOW + "Choose an option: " + UIHelper.RESET);
            String option = scanner.nextLine().trim();

            List<com.entities.Wishlist> selections = new ArrayList<>();
            switch (option) {
                case "1" -> selections.addAll(wishlistEntries);
                case "2" -> {
                    System.out.println("Enter item IDs separated by commas (e.g., 2,5,9): ");
                    String input = scanner.nextLine().trim();
                    if (input.isBlank()) {
                        System.out.println(UIHelper.RED + "You must specify at least one item." + UIHelper.RESET);
                        continue;
                    }
                    // Parse and validate IDs
                    Set<Integer> requestedIds = parseIdList(input);
                    if (requestedIds.isEmpty()) {
                        System.out.println(UIHelper.RED + "Invalid list of IDs provided." + UIHelper.RESET);
                        continue;
                    }
                    // Make sure every ID exists in wishlist
                    Set<Integer> wishlistIds = new HashSet<>();
                    for (com.entities.Wishlist w : wishlistEntries) {
                        wishlistIds.add(w.getItemId());
                    }
                    List<Integer> missing = new ArrayList<>();
                    for (Integer id : requestedIds) {
                        if (!wishlistIds.contains(id)) {
                            missing.add(id);
                        }
                    }
                    if (!missing.isEmpty()) {
                        System.out.println(UIHelper.RED + "These items are not in your wishlist: " + missing
                                + UIHelper.RESET);
                        System.out.println(UIHelper.YELLOW
                                + "Please enter only item IDs that are in your wishlist." + UIHelper.RESET);
                        continue;
                    }
                    for (Integer id : requestedIds) {
                        wishlistEntries.stream()
                                .filter(w -> w.getItemId() == id)
                                .findFirst()
                                .ifPresent(selections::add);
                    }
                    if (selections.isEmpty()) {
                        System.out.println(UIHelper.RED + "Selected items were not found in your wishlist."
                                + UIHelper.RESET);
                        continue;
                    }
                }
                case "3" -> {
                    return false;
                }
                default -> {
                    System.out.println(UIHelper.RED + "Invalid option." + UIHelper.RESET);
                    continue;
                }
            }

            // Build order items from wishlist
            List<OrderItemRequest> orderItems = new ArrayList<>();
            for (com.entities.Wishlist w : selections) {
                Item item = itemMap.get(w.getItemId());
                if (item == null) {
                    System.out.println(UIHelper.RED + "Item #" + w.getItemId() + " is no longer available."
                            + UIHelper.RESET);
                    continue;
                }
                int qty = Math.max(1, w.getQuantity());
                if (item.getStockQuantity() < qty) {
                    System.out.println(UIHelper.RED + "Insufficient stock for " + item.getName() + "." + UIHelper.RESET);
                    continue;
                }
                orderItems.add(new OrderItemRequest(item.getId(), qty));
            }

            if (orderItems.isEmpty()) {
                System.out.println(UIHelper.RED
                        + "No valid items available to purchase (check stock/availability)." + UIHelper.RESET);
                continue;
            }

            // Compute total and show summary
            double total = 0;
            System.out.println(UIHelper.CYAN + "--- PURCHASE SUMMARY ---" + UIHelper.RESET);
            for (OrderItemRequest req : orderItems) {
                Item detail = itemMap.get(req.getItemId());
                double price = detail != null ? detail.getPrice() : 0;
                String name = detail != null ? detail.getName() : ("Item #" + req.getItemId());
                double sub = price * req.getQuantity();
                total += sub;
                System.out.printf(" - %s (x%d) @ $%.2f -> $%.2f%n", name, req.getQuantity(), price, sub);
            }
            System.out.printf("TOTAL: $%.2f%n", total);

            if (!ensurePaymentCardForCheckout(scanner, broker)) {
                return false;
            }
            String shippingAddress = promptShippingAddress(scanner);

            while (true) {
                System.out.println("1. Confirm purchase");
                System.out.println("2. Cancel");
                System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);
                String confirm = scanner.nextLine().trim();
                if ("1".equals(confirm)) {
                    OrderCreateRequest req = new OrderCreateRequest(Main.currentUser.getId(), orderItems,
                            shippingAddress);
                    broker.publish(EventType.PURCHASE_REQUESTED, req);
                    UIHelper.loading("Processing payment");
                    System.out.println(UIHelper.GREEN + "Purchase requested. Check notifications for confirmation."
                            + UIHelper.RESET);
                    return true;
                } else if ("2".equals(confirm)) {
                    System.out.println(UIHelper.YELLOW + "Purchase canceled." + UIHelper.RESET);
                    return false;
                } else {
                    System.out.println(UIHelper.RED + "Invalid option." + UIHelper.RESET);
                }
            }
        }
    }

    private static Set<Integer> parseIdList(String raw) {
        Set<Integer> ids = new HashSet<>();
        if (raw == null)
            return ids;
        String[] parts = raw.split(",");
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty())
                continue;
            try {
                int v = Integer.parseInt(t);
                if (v <= 0) {
                    System.out.println(UIHelper.RED + "Invalid ID: " + t + UIHelper.RESET);
                    return new HashSet<>();
                }
                ids.add(v);
            } catch (NumberFormatException ex) {
                System.out.println(UIHelper.RED + "Invalid ID: " + t + UIHelper.RESET);
                return new HashSet<>();
            }
        }
        return ids;
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
        int itemId;
        try {
            itemId = Integer.parseInt(scanner.nextLine().trim());
            if (itemId <= 0) {
                System.out.println(UIHelper.RED + "Item ID must be positive." + UIHelper.RESET);
                return false;
            }
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid item ID. Please enter a valid number." + UIHelper.RESET);
            return false;
        }

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
            int quantity;
            try {
                quantity = Integer.parseInt(scanner.nextLine().trim());
                if (quantity <= 0) {
                    System.out.println(UIHelper.RED + "Quantity must be positive." + UIHelper.RESET);
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println(UIHelper.RED + "Invalid quantity. Please enter a valid number." + UIHelper.RESET);
                continue;
            }

            if (quantity > item.getStockQuantity()) {
                System.out.println(UIHelper.RED + "Insufficient stock available." + UIHelper.RESET);
                continue;
            }

            double total = item.getPrice() * quantity;
            System.out.printf("Total Amount: $%.2f%n", total);

            // Require a valid payment card before proceeding
            if (!ensurePaymentCardForCheckout(scanner, broker)) {
                return false;
            }

            // Require billing/shipping address
            String shippingAddress = promptShippingAddress(scanner);

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
                            shippingAddress);
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

    private static String promptShippingAddress(Scanner scanner) {
        while (true) {
            System.out.print("Enter shipping/billing address: ");
            String address = scanner.nextLine().trim();
            if (!address.isBlank()) {
                return address;
            }
            System.out.println(UIHelper.RED + "Address cannot be empty." + UIHelper.RESET);
        }
    }

    private static boolean requireLoggedInForAction(String action) {
        if (Main.currentUser != null) {
            return true;
        }
        System.out.println(UIHelper.RED + "Please log in to " + action + "." + UIHelper.RESET);
        return false;
    }

    // ======================================================
    // PAYMENT CARD HELPERS (checkout + add-card validation)
    // ======================================================
    private static boolean ensurePaymentCardForCheckout(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null) {
            System.out.println(UIHelper.RED + "You must be logged in to use payment cards." + UIHelper.RESET);
            return false;
        }

        List<PaymentCard> cards = BrokerUtils.requestOnce(
                broker,
                EventType.PAYMENT_CARD_LIST_REQUESTED,
                Main.currentUser.getId(),
                EventType.PAYMENT_CARD_LIST_RETURNED,
                3000);

        if (cards != null && !cards.isEmpty()) {
            // Already have at least one saved card – allow checkout
            return true;
        }

        System.out.println(UIHelper.YELLOW
                + "No payment cards on file. Please enter a card to continue with checkout." + UIHelper.RESET);

        String cardType = promptCardType(scanner);
        String cardNumber = readValidCardNumberForType(scanner, cardType);

        System.out.print("Cardholder name: ");
        String holderName = scanner.nextLine().trim();
        String expiry = readValidCardExpiry(scanner);

        while (true) {
            System.out.print("Save this card for future purchases? (y/n): ");
            String save = scanner.nextLine().trim().toLowerCase();
            if ("y".equals(save) || "yes".equals(save)) {
                com.common.dto.payment.PaymentCardAddRequest request =
                        new com.common.dto.payment.PaymentCardAddRequest(
                                Main.currentUser.getId(),
                                holderName,
                                cardNumber,
                                expiry,
                                cardType);
                broker.publish(EventType.PAYMENT_CARD_ADD_REQUESTED, request);
                System.out.println(UIHelper.GREEN + "Card saved to your account." + UIHelper.RESET);
                break;
            } else if ("n".equals(save) || "no".equals(save)) {
                System.out.println(UIHelper.YELLOW + "Card will be used for this purchase only." + UIHelper.RESET);
                break;
            } else {
                System.out.println(UIHelper.RED + "Invalid option. Please answer y/n." + UIHelper.RESET);
            }
        }

        return true;
    }

    private static String promptCardType(Scanner scanner) {
        while (true) {
            System.out.println("Card Type:");
            System.out.println("1. VISA");
            System.out.println("2. MasterCard");
            System.out.println("3. American Express");
            System.out.println("4. Discover");
            System.out.println("5. JCB");
            System.out.print(UIHelper.YELLOW + "Select card type: " + UIHelper.RESET);
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    return "VISA";
                case "2":
                    return "MASTERCARD";
                case "3":
                    return "AMEX";
                case "4":
                    return "DISCOVER";
                case "5":
                    return "JCB";
                default:
                    System.out.println(UIHelper.RED + "Invalid option." + UIHelper.RESET);
            }
        }
    }

    private static String readValidCardNumberForType(Scanner scanner, String cardType) {
        while (true) {
            System.out.print("Card number: ");
            String digits = scanner.nextLine().replaceAll("\\s+", "");
            if (!digits.matches("\\d+")) {
                System.out.println(UIHelper.RED + "Card number must contain only digits." + UIHelper.RESET);
                continue;
            }

            boolean ok = switch (cardType) {
                case "VISA" -> digits.length() == 16 && digits.startsWith("4");
                case "MASTERCARD" -> digits.length() == 16 && digits.startsWith("5");
                case "AMEX" -> digits.length() == 15 && digits.startsWith("3");
                case "DISCOVER" -> digits.length() == 16 && digits.startsWith("6");
                case "JCB" -> digits.length() == 16;
                default -> digits.length() >= 13 && digits.length() <= 19;
            };

            if (!ok) {
                System.out.println(UIHelper.RED + "Invalid number for card type " + cardType + "." + UIHelper.RESET);
                continue;
            }

            long distinct = digits.chars().distinct().count();
            if (distinct == 1) {
                System.out.println(UIHelper.RED + "Card number cannot use the same digit repeated." + UIHelper.RESET);
                continue;
            }

            return digits;
        }
    }

    private static String readValidCardExpiry(Scanner scanner) {
        while (true) {
            System.out.print("Expiry (MM/YY): ");
            String expiry = scanner.nextLine().trim();
            if (!expiry.matches("^(0[1-9]|1[0-2])\\/\\d{2}$")) {
                System.out.println(UIHelper.RED + "Expiry must match MM/YY (e.g., 04/27)." + UIHelper.RESET);
                continue;
            }
            try {
                int month = Integer.parseInt(expiry.substring(0, 2));
                int year = Integer.parseInt(expiry.substring(3, 5));
                int fullYear = 2000 + year;
                java.time.YearMonth expYm = java.time.YearMonth.of(fullYear, month);
                java.time.YearMonth nowYm = java.time.YearMonth.now();
                if (!expYm.isAfter(nowYm)) {
                    System.out.println(UIHelper.RED + "Card has expired. Please use a valid card." + UIHelper.RESET);
                    continue;
                }
            } catch (Exception ex) {
                System.out.println(UIHelper.RED + "Invalid expiry date." + UIHelper.RESET);
                continue;
            }
            return expiry;
        }
    }

    public static void search(Scanner scanner, AsyncMessageBroker broker) {
        System.out.print("Enter keywords: ");
        String keyword = scanner.nextLine().trim();

        // Allow empty search to show all items
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

        List<com.entities.Wishlist> wishlistEntries = new ArrayList<>();
        if (wl != null) {
            for (Object o : wl) {
                if (o instanceof com.entities.Wishlist w) {
                    wishlistEntries.add(w);
                }
            }
        }

        if (wishlistEntries.isEmpty()) {
            System.out.println(UIHelper.YELLOW + "Your wishlist is empty." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Get all items to map ids to details
        List<Item> items = BrokerUtils.requestOnce(broker, EventType.ITEM_BROWSE_REQUESTED, null,
                EventType.ITEM_LIST_RETURNED, 3000);

        List<String> boxLines = new ArrayList<>();
        for (com.entities.Wishlist w : wishlistEntries) {
            Item it = null;
            if (items != null) {
                it = items.stream().filter(x -> x.getId() == w.getItemId()).findFirst().orElse(null);
            }
            if (it != null) {
                boxLines.add(String.format("#%d %s - $%.2f (qty %d)",
                        it.getId(), it.getName(), it.getPrice(), Math.max(1, w.getQuantity())));
            } else {
                boxLines.add(String.format("#%d (item data unavailable) qty %d",
                        w.getItemId(), Math.max(1, w.getQuantity())));
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
                    if (handleWishlistPurchase(scanner, broker, wishlistEntries, items)) {
                        UIHelper.pause();
                        return;
                    }
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
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i);
            int displayId = i + 1;
            summaryLines.add(String.format("#%d | Total: $%.2f | Status: %s",
                    displayId,
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
                    String input = scanner.nextLine().trim();
                    Integer idx = parsePositiveInt(input, "order ID");
                    if (idx == null || idx < 1 || idx > orders.size()) {
                        System.out.println(UIHelper.RED + "Order not found." + UIHelper.RESET);
                        continue;
                    }

                    Order order = orders.get(idx - 1);

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

        // Use order's billing address if available
        String billingAddress = order.getBillingAddress();
        if (billingAddress == null || billingAddress.isBlank()) {
            billingAddress = "Pending Address";
        }
        detailLines.add(String.format("Billing Address: %s", billingAddress));
        detailLines.add("");
        detailLines.add("Items:");

        // Fetch order items from repository and item details
        boolean printedItems = false;
        try {
            // Use the repositories from Main instead of creating new instances
            List<com.entities.OrderItem> orderItems = Main.getOrderItemRepository().findByOrderId(order.getId());

            if (orderItems != null && !orderItems.isEmpty()) {
                for (com.entities.OrderItem orderItem : orderItems) {
                    com.entities.Item item = Main.getItemRepository().findById(orderItem.getItemId());
                    if (item != null) {
                        detailLines.add(String.format("  - %s", item.getName()));
                        if (item.getDescription() != null && !item.getDescription().isBlank()) {
                            detailLines.add(String.format("    %s", item.getDescription()));
                        }
                        detailLines.add(String.format("    Quantity: %d | Price: $%.2f | Subtotal: $%.2f",
                                orderItem.getQuantity(), orderItem.getPriceAtPurchase(),
                                orderItem.getQuantity() * orderItem.getPriceAtPurchase()));
                        detailLines.add("");
                        printedItems = true;
                    }
                }
            }
        } catch (Exception e) {
            // Fall back to showing basic message if repository access fails
        }

        if (!printedItems) {
            detailLines.add("(Item details not available)");
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
        openChatWithStaff(scanner, broker);
    }

    // =========================
    // CUSTOMER–STAFF CHAT
    // =========================
    private static void openChatWithStaff(Scanner scanner, AsyncMessageBroker broker) {
        int userId = Main.currentUser.getId();

        // Ask if starting a brand new conversation
        System.out.print("Start a new conversation? (y/n): ");
        String ans = scanner.nextLine().trim().toLowerCase();
        if ("y".equals(ans) || "yes".equals(ans)) {
            System.out.print("Subject: ");
            String subject = scanner.nextLine().trim();
            if (subject.isEmpty()) {
                System.out.println(UIHelper.RED + "Subject cannot be empty." + UIHelper.RESET);
                UIHelper.pause();
                return;
            }
            System.out.print("Your Message: ");
            String msg = scanner.nextLine().trim();
            if (msg.isEmpty()) {
                System.out.println(UIHelper.RED + "Message cannot be empty." + UIHelper.RESET);
                UIHelper.pause();
                return;
            }
            MessageSendRequest req = new MessageSendRequest(userId, -1, subject, msg);
            broker.publish(EventType.MESSAGE_SEND_REQUESTED, req);
            System.out.println(UIHelper.GREEN + "Message sent to staff." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        while (true) {
            UIHelper.clear();
            System.out.println(UIHelper.CYAN + "=== CHAT WITH STAFF ===" + UIHelper.RESET);

            List<com.entities.UserMessage> history = BrokerUtils.requestOnce(
                    broker,
                    EventType.CONVERSATION_LOAD_REQUESTED,
                    new com.common.dto.message.ConversationLoadRequest(userId, -1),
                    EventType.CONVERSATION_MESSAGES_RETURNED,
                    3000);

            if (history == null || history.isEmpty()) {
                System.out.println(UIHelper.YELLOW + "(No previous messages – start a new one.)" + UIHelper.RESET);
            } else {
                for (com.entities.UserMessage m : history) {
                    String tag = (m.getSenderId() == userId) ? "Me" : "Staff";
                    System.out.println("[" + tag + "]: " + m.getContent());
                }
            }

            System.out.println("\nActions:");
            System.out.println("1. Send message");
            System.out.println("2. Back");
            System.out.print(UIHelper.YELLOW + "Select: " + UIHelper.RESET);
            String option = scanner.nextLine().trim();

            if ("1".equals(option)) {
                System.out.print("Your Message (leave blank to cancel): ");
                String reply = scanner.nextLine().trim();
                if (reply.isEmpty()) {
                    System.out.println(UIHelper.YELLOW + "No message sent." + UIHelper.RESET);
                    UIHelper.pause();
                    continue;
                }
                MessageSendRequest resp = new MessageSendRequest(userId, -1, "Reply", reply);
                broker.publish(EventType.MESSAGE_SEND_REQUESTED, resp);
                System.out.println(UIHelper.GREEN + "Message sent to staff." + UIHelper.RESET);
                UIHelper.pause();
            } else if ("2".equals(option)) {
                return;
            } else {
                System.out.println(UIHelper.RED + "Invalid option." + UIHelper.RESET);
                UIHelper.pause();
            }
        }
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

        for (int i = 0; i < notifications.size(); i++) {
            System.out.println((i + 1) + ") " + notifications.get(i));
        }
        System.out.println("0) Back");

        System.out.println();
        System.out.println("Enter notification number to open chat, 'c' to clear all, or Enter to go back:");
        String input = scanner.nextLine().trim().toLowerCase();
        if ("c".equals(input)) {
            Main.clearNotifications();
            System.out.println(UIHelper.YELLOW + "Notifications cleared." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }
        if (input.isEmpty() || "0".equals(input)) {
            return;
        }
        int idx;
        try {
            idx = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }
        if (idx < 1 || idx > notifications.size()) {
            System.out.println(UIHelper.RED + "Invalid selection." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        String selected = notifications.get(idx - 1);

        // Nếu là thông báo tin nhắn từ staff -> mở boxchat, sau đó xóa noti
        if (selected != null && selected.toLowerCase().contains("new reply from staff")) {
            openChatWithStaff(scanner, broker);
        } else {
            // Các thông báo hệ thống chỉ hiển thị nội dung
            System.out.println(UIHelper.YELLOW + "Notification: " + selected + UIHelper.RESET);
        }

        // Sau khi đã xem bất kỳ notification nào thì xóa nó khỏi danh sách
        Main.removeNotification(idx - 1);
        UIHelper.pause();
    }

    public static void viewAccount(Scanner scanner, AsyncMessageBroker broker) {
        if (Main.currentUser == null) {
            System.out.println(UIHelper.RED + "No user is currently logged in." + UIHelper.RESET);
            UIHelper.pause();
            return;
        }

        // Request authoritative account info from account subsystem
        User user = BrokerUtils.requestOnce(broker, EventType.ACCOUNT_VIEW_REQUESTED,
                new com.common.dto.account.AccountViewRequest(Main.currentUser.getId()),
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
            for (int i = 0; i < cards.size(); i++) {
                PaymentCard card = cards.get(i);
                int displayIndex = i + 1;
                cardLines.add(String.format("Card #%d | Type: %s (%s)",
                        displayIndex,
                        card.getCardType().equals("DEBIT") ? "Debit Card" : "Credit Card",
                        card.getCardType()));
                cardLines.add(String.format("Number: **** **** **** %s",
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

    private static void editAccountMenu(Scanner scanner, AsyncMessageBroker broker, User currentDisplayUser) {
        while (true) {
            System.out.println(UIHelper.YELLOW + "Edit Account:" + UIHelper.RESET);
            System.out.println("1. Edit username");
            System.out.println("2. Edit phone");
            System.out.println("3. Edit Address");
            System.out.println("4. Edit password");
            System.out.println("5. Remove payment card");
            System.out.println("6. Add payment card");
            System.out.println("7. Back");
            System.out.print(UIHelper.YELLOW + "Select an option: " + UIHelper.RESET);
            String c = scanner.nextLine().trim();

            switch (c) {
                case "1" -> {
                    System.out.print("New username: ");
                    String v = scanner.nextLine().trim();
                    if (v.isEmpty()) {
                        System.out.println(UIHelper.RED + "Username cannot be empty." + UIHelper.RESET);
                        continue;
                    }
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
                    if (v.isEmpty()) {
                        System.out.println(UIHelper.RED + "Phone cannot be empty." + UIHelper.RESET);
                        continue;
                    }
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
                    if (v.isEmpty()) {
                        System.out.println(UIHelper.RED + "Address cannot be empty." + UIHelper.RESET);
                        continue;
                    }
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
                    // Remove payment card with confirmation
                    List<PaymentCard> cards = BrokerUtils.requestOnce(broker,
                            EventType.PAYMENT_CARD_LIST_REQUESTED,
                            currentDisplayUser.getId(),
                            EventType.PAYMENT_CARD_LIST_RETURNED,
                            3000);
                    if (cards == null || cards.isEmpty()) {
                        System.out.println(UIHelper.YELLOW + "No payment cards available." + UIHelper.RESET);
                        UIHelper.pause();
                        continue;
                    }

                    System.out.println(UIHelper.CYAN + "--- Your Cards ---" + UIHelper.RESET);
                    for (PaymentCard card : cards) {
                        System.out.printf("ID %d: **** **** **** %s (Exp: %s)%n",
                                card.getId(),
                                card.getLast4Digits(),
                                card.getExpiryDate());
                    }

                    System.out.print("Enter card ID to remove (or 0 to cancel): ");
                    String idStr = scanner.nextLine().trim();
                    int cardId;
                    try {
                        cardId = Integer.parseInt(idStr);
                    } catch (NumberFormatException e) {
                        System.out.println(UIHelper.RED + "Invalid card ID" + UIHelper.RESET);
                        continue;
                    }
                    if (cardId <= 0) {
                        continue;
                    }

                    PaymentCard selected = null;
                    for (PaymentCard card : cards) {
                        if (card.getId() == cardId) {
                            selected = card;
                            break;
                        }
                    }
                    if (selected == null) {
                        System.out.println(UIHelper.RED + "Card ID not found." + UIHelper.RESET);
                        continue;
                    }

                    System.out.print("Are you sure you want to delete this card? (y/n): ");
                    String yn = scanner.nextLine().trim().toLowerCase();
                    if ("y".equals(yn) || "yes".equals(yn)) {
                        broker.publish(EventType.PAYMENT_CARD_REMOVE_REQUESTED, cardId);
                        System.out.println(UIHelper.GREEN + "Payment card removed successfully!" + UIHelper.RESET);
                        UIHelper.pause();
                        viewAccount(scanner, broker);
                        return;
                    }
                }
                case "6" -> {
                    // Add payment card with type-specific validation
                    System.out.println(UIHelper.CYAN + "\n=== Add Payment Card ===" + UIHelper.RESET);

                    String cardType = promptCardType(scanner);
                    String cardNum = readValidCardNumberForType(scanner, cardType);

                    // Cardholder Name (2-50 characters)
                    String cardName;
                    while (true) {
                        System.out.print("Cardholder name (2-50 characters): ");
                        cardName = scanner.nextLine().trim();
                        if (cardName.length() >= 2 && cardName.length() <= 50) {
                            break;
                        } else {
                            System.out
                                    .println(UIHelper.RED + "Invalid name! Must be 2-50 characters." + UIHelper.RESET);
                        }
                    }

                    String expiry = readValidCardExpiry(scanner);

                    // Create and publish payment card add request
                    com.common.dto.payment.PaymentCardAddRequest cardRequest = new com.common.dto.payment.PaymentCardAddRequest(
                            currentDisplayUser.getId(), cardName, cardNum, expiry, cardType);
                    broker.publish(EventType.PAYMENT_CARD_ADD_REQUESTED, cardRequest);
                    System.out.println(UIHelper.GREEN + "Payment card added successfully!" + UIHelper.RESET);
                    UIHelper.pause();
                    viewAccount(scanner, broker);
                    return;
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
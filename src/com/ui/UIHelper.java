package com.ui;

import java.util.List;

public class UIHelper {

    // ANSI COLORS
    public static final String RESET = "\u001B[0m";
    public static final String BLUE = "\u001B[34m";
    public static final String GREEN = "\u001B[32m";
    public static final String CYAN = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String RED = "\u001B[31m";
    public static final String MAGENTA = "\u001B[35m";

    // Clear screen
    public static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static final int BOX_WIDTH = 50;
    private static final String ANSI_REGEX = "\u001B\\[[;\\d]*m";

    // Boxed screen
    public static void box(String title, List<String> lines) {
        String border = "┌" + "─".repeat(BOX_WIDTH) + "┐";
        String footer = "└" + "─".repeat(BOX_WIDTH) + "┘";

        System.out.println(border);
        System.out.printf("│ %s │%n", padForBox(title));
        System.out.println("├" + "─".repeat(BOX_WIDTH) + "┤");

        for (String line : lines) {
            System.out.printf("│ %s │%n", padForBox(line));
        }

        System.out.println(footer);
    }

    private static String padForBox(String text) {
        String stripped = text.replaceAll(ANSI_REGEX, "");
        int printableLength = stripped.length();
        if (printableLength >= BOX_WIDTH) {
            return text;
        }
        return text + " ".repeat(BOX_WIDTH - printableLength);
    }

    // Loading animation
    public static void loading(String message) {
        System.out.print(message);
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(300);
                System.out.print(".");
            } catch (Exception ignored) {
            }
        }
        System.out.println();
    }

    // Pause until ENTER
    public static void pause() {
        System.out.println("\nPress ENTER to continue...");
        try {
            // Read single character instead of using Scanner
            System.in.read();
            // Clear any remaining characters in buffer
            while (System.in.available() > 0) {
                System.in.read();
            }
        } catch (Exception ignored) {
        }
    }

    // Colored text
    public static String color(String text, String c) {
        return c + text + RESET;
    }
}
package com.entities;

/**
 * Represents a product/item in the system.
 *
 * Business Rules:
 * 1. Wishlist add logic should verify stockQuantity > 0 before allowing
 * addition.
 * 2. Items can be sorted by likeCount (for features like popular items).
 */

public class Item {
    private int id;
    private String name;
    private String description;
    private double price;
    private int stockQuantity;
    private int likeCount;

    public Item() {

    };

    public Item(int id, String name, String description, double price, int stockQuantity, int likeCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.likeCount = likeCount;
    }

    public int getID() {
        return id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean canAddToWishList() {
        return stockQuantity > 0;
    }
}

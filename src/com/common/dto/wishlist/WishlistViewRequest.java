package com.common.dto.wishlist;

public class WishlistViewRequest {

    private final int userId;

    public WishlistViewRequest(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public String toString() {
        return "WishlistViewRequest{userId=" + userId + '}';
    }
}
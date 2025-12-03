package com.managers.wishlist;

import com.entities.Item;
import com.entities.Wishlist;
import com.repository.ItemRepository;
import com.repository.WishlistRepository;

import java.util.ArrayList;
import java.util.List;

public class ViewWishlistManager {

    private final WishlistRepository wishlistRepo;
    private final ItemRepository itemRepo;

    public ViewWishlistManager(WishlistRepository wishlistRepo, ItemRepository itemRepo) {
        this.wishlistRepo = wishlistRepo;
        this.itemRepo = itemRepo;
    }

    /**
     * Return formatted wishlist entries for display
     */
    public List<String> viewWishlist(int userId) {

        List<Wishlist> list = wishlistRepo.findByCustomerId(userId);

        List<String> results = new ArrayList<>();

        for (Wishlist wl : list) {
            Item item = itemRepo.findById(wl.getItemId());

            if (item != null) {
                String entry = String.format(
                        "Item: %s | Price: %.2f | Added: %d",
                        item.getName(),
                        item.getPrice(),
                        wl.getAddedAt());
                results.add(entry);
            }
        }

        return results;
    }
}
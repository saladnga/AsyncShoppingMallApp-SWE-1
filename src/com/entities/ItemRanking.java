package com.entities;

/**
 * Represents a ranked item (for Top Ranking features)
 */

public class ItemRanking {

    private int itemId;
    private String itemName;
    private int likeCount;
    private int rank;

    public ItemRanking(int itemId, String itemName, int likeCount, int rank) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.likeCount = likeCount;
        this.rank = rank;
    }

    public int getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return "ItemRanking{" +
                "itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", likeCount=" + likeCount +
                ", rank=" + rank +
                '}';
    }
}
package com.common.dto.item;

/**
 * DTO used when the system or user requests ranked items.
 * RankingManager will:
 * - compute ranking based on likes
 * - publish ITEM_RANKING_RETURNED or ITEM_LIST_RETURNED
 */

public class ItemRankingRequest {

    private final String category;
    private final int limit;

    public ItemRankingRequest(String category, int limit) {
        this.category = category;
        this.limit = limit;
    }

    public ItemRankingRequest(int limit) {
        this(null, limit);
    }

    public String getCategory() {
        return category;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String toString() {
        return "ItemRankingRequest{" +
                "category='" + category + '\'' +
                ", limit=" + limit +
                '}';
    }
}
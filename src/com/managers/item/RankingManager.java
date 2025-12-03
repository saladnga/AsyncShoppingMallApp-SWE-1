package com.managers.item;

import com.entities.ItemRanking;
import com.repository.ItemRankingRepository;
import com.repository.ItemRepository;

import java.util.List;

/**
 * Computes top ranked items (based on likes or sales).
 */

public class RankingManager {

    private final ItemRankingRepository rankingRepo;
    private final ItemRepository itemRepo;

    public RankingManager(ItemRankingRepository rankingRepo, ItemRepository itemRepo) {
        this.rankingRepo = rankingRepo;
        this.itemRepo = itemRepo;
    }

    public List<ItemRanking> updateRanking() {

        // Compute top items (example: by like count)
        List<ItemRanking> ranking = itemRepo.computeRanking();

        // Save ranking
        rankingRepo.replaceAll(ranking);

        return ranking;
    }
}
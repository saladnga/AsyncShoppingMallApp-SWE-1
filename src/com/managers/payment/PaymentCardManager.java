package com.managers.payment;

import com.entities.PaymentCard;
import com.repository.PaymentCardRepository;
import java.util.List;

public class PaymentCardManager {

    private final PaymentCardRepository repo;

    public PaymentCardManager(PaymentCardRepository repo) {
        this.repo = repo;
    }

    public List<PaymentCard> getCardsForUser(int userId) {
        return repo.findByUserId(userId);
    }

    public PaymentCard getDefaultCard(int userId) {
        return repo.findDefaultCard(userId);
    }

    public void addNewCard(PaymentCard card) {
        repo.insert(card);
    }

    public void setDefault(int cardId) {
        // FIXED: repo requires userId + cardId
        PaymentCard card = repo.findById(cardId);
        if (card != null) {
            repo.setDefaultCard(card.getUserId(), cardId);
        }
    }

    public void removeCard(int cardId) {
        repo.delete(cardId);
    }
}
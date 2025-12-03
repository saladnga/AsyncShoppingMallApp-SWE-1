package com.repository;

import com.entities.PaymentCard;
import java.util.List;

public interface PaymentCardRepository {

    // ================================
    // REQUIRED BY MANAGERS
    // ================================

    /** Insert or update and return ID */
    default int save(PaymentCard card) {
        if (card.getId() == 0) {
            insert(card);
        } else {
            update(card);
        }
        return card.getId();
    }

    /** Manager calls setDefault(cardId) */
    default void setDefaultCard(int cardId) {
        throw new UnsupportedOperationException("setDefaultCard must be implemented");
    }

    // ================================
    // BASE REPOSITORY
    // ================================

    PaymentCard findById(int id);

    List<PaymentCard> findByUserId(int userId);

    void insert(PaymentCard card);

    void update(PaymentCard card);

    void delete(int id);

    PaymentCard findDefaultCard(int userId);

    /** Original version requiring both values */
    void setDefaultCard(int userId, int cardId);

    boolean isCardExpired(PaymentCard card);
}
package com.managers.payment;

import com.entities.PaymentTransaction;
import com.repository.PaymentTransactionRepository;

public class PaymentTransactionManager {

    private final PaymentTransactionRepository repo;

    public PaymentTransactionManager(PaymentTransactionRepository repo) {
        this.repo = repo;
    }

    public void updateStatus(PaymentTransaction transaction,
            PaymentTransaction.Status newStatus) {
        transaction.setStatus(newStatus);
        repo.update(transaction);
    }

    public PaymentTransaction getById(int id) {
        return repo.findById(id);
    }
}
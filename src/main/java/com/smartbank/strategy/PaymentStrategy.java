package com.smartbank.strategy;

import com.smartbank.entity.Transaction;

public interface PaymentStrategy {
    boolean processPayment(Transaction transaction);
    boolean validatePayment(Transaction transaction);
    String getStrategyName();
}

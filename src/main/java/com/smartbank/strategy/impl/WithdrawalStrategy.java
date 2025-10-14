package com.smartbank.strategy.impl;

import com.smartbank.entity.Transaction;
import com.smartbank.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
public class WithdrawalStrategy implements PaymentStrategy {

    @Override
    public boolean validatePayment(Transaction transaction) {
        return transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                transaction.getFromAccountId() != null;
    }

    @Override
    public boolean processPayment(Transaction transaction) {
        log.info("Processing withdrawal of {} from account {}",
                transaction.getAmount(), transaction.getFromAccountId());

        if (!validatePayment(transaction)) {
            log.error("Withdrawal validation failed");
            return false;
        }

        // Business logic for withdrawal
        log.info("Withdrawal processed successfully");
        return true;
    }

    @Override
    public String getStrategyName() {
        return "WITHDRAWAL";
    }
}
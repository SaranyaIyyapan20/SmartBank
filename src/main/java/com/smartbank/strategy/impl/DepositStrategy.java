package com.smartbank.strategy.impl;

import com.smartbank.entity.Transaction;
import com.smartbank.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
public class DepositStrategy implements PaymentStrategy {

    @Override
    public boolean validatePayment(Transaction transaction) {
        return transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                transaction.getToAccountId() != null;
    }

    @Override
    public boolean processPayment(Transaction transaction) {
        log.info("Processing deposit of {} to account {}",
                transaction.getAmount(), transaction.getToAccountId());

        if (!validatePayment(transaction)) {
            log.error("Deposit validation failed");
            return false;
        }

        // Business logic for deposit
        // In real implementation, this would integrate with core banking
        log.info("Deposit processed successfully");
        return true;
    }

    @Override
    public String getStrategyName() {
        return "DEPOSIT";
    }
}
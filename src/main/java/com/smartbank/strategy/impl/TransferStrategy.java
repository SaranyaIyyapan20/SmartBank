package com.smartbank.strategy.impl;

import com.smartbank.entity.Transaction;
import com.smartbank.strategy.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;

@Slf4j
public class TransferStrategy implements PaymentStrategy {

    @Override
    public boolean validatePayment(Transaction transaction) {
        return transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                transaction.getFromAccountId() != null &&
                transaction.getToAccountId() != null &&
                !transaction.getFromAccountId().equals(transaction.getToAccountId());
    }

    @Override
    public boolean processPayment(Transaction transaction) {
        log.info("Processing transfer of {} from {} to {}",
                transaction.getAmount(),
                transaction.getFromAccountId(),
                transaction.getToAccountId());

        if (!validatePayment(transaction)) {
            log.error("Transfer validation failed");
            return false;
        }

        // Business logic for transfer
        // Debit from source, credit to destination
        log.info("Transfer processed successfully");
        return true;
    }

    @Override
    public String getStrategyName() {
        return "TRANSFER";
    }
}
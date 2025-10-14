package com.smartbank.validation;

import com.smartbank.entity.Transaction;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public class AmountValidator extends TransactionValidator {

    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");
    private static final BigDecimal MIN_TRANSACTION_AMOUNT = new BigDecimal("0.01");

    @Override
    public boolean validate(Transaction transaction) {
        log.debug("Validating amount for transaction: {}", transaction.getTransactionReference());
        BigDecimal amount = transaction.getAmount();

        if (amount == null) {
            transaction.setErrorMessage("Transaction amount cannot be null");
            return false;
        }

        if (amount.compareTo(MIN_TRANSACTION_AMOUNT) < 0) {
            transaction.setErrorMessage("Amount must be greater than " + MIN_TRANSACTION_AMOUNT);
            return false;
        }

        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            transaction.setErrorMessage("Amount exceeds maximum limit of " + MAX_TRANSACTION_AMOUNT);
            return false;
        }

        return validateNext(transaction);
    }
}

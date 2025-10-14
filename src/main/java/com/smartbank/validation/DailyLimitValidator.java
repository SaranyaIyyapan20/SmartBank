package com.smartbank.validation;

import com.smartbank.entity.Transaction;
import com.smartbank.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
public class DailyLimitValidator extends TransactionValidator {

    private final TransactionRepository transactionRepository;
    private static final BigDecimal DAILY_LIMIT = new BigDecimal("500000.00");

    public DailyLimitValidator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public boolean validate(Transaction transaction) {
        log.debug("Validating daily limit for transaction: {}", transaction.getTransactionReference());

        if (transaction.getFromAccountId() == null) {
            return validateNext(transaction);
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        BigDecimal todaysTotalDebits = transactionRepository
                .getTotalDebitsSince(transaction.getFromAccountId(), startOfDay);

        if (todaysTotalDebits == null) {
            todaysTotalDebits = BigDecimal.ZERO;
        }

        BigDecimal totalAfterTransaction = todaysTotalDebits.add(transaction.getAmount());

        if (totalAfterTransaction.compareTo(DAILY_LIMIT) > 0) {
            transaction.setErrorMessage(String.format(
                    "Daily transaction limit exceeded. Limit: %s, Today's total: %s, Attempted: %s",
                    DAILY_LIMIT, todaysTotalDebits, transaction.getAmount()
            ));
            return false;
        }

        return validateNext(transaction);
    }
}

package com.smartbank.validation;

import com.smartbank.entity.Account;
import com.smartbank.entity.Transaction;
import com.smartbank.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
public class BalanceValidator extends TransactionValidator {

    private final AccountRepository accountRepository;

    public BalanceValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public boolean validate(Transaction transaction) {
        log.debug("Validating balance for transaction: {}", transaction.getTransactionReference());

        if (transaction.getFromAccountId() == null) {
            return validateNext(transaction);
        }

        Optional<Account> accountOpt = accountRepository.findById(transaction.getFromAccountId());
        if (!accountOpt.isPresent()) {
            transaction.setErrorMessage("Source account not found: " + transaction.getFromAccountId());
            return false;
        }

        Account account = accountOpt.get();
        BigDecimal currentBalance = account.getBalance();
        BigDecimal requiredAmount = transaction.getAmount();

        if (currentBalance.compareTo(requiredAmount) < 0) {
            transaction.setErrorMessage(String.format(
                    "Insufficient balance. Available: %s, Required: %s",
                    currentBalance, requiredAmount
            ));
            return false;
        }

        return validateNext(transaction);
    }
}

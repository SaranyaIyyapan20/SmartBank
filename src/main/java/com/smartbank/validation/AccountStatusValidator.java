package com.smartbank.validation;

import com.smartbank.entity.Account;
import com.smartbank.enums.AccountStatus;
import com.smartbank.entity.Transaction;
import com.smartbank.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class AccountStatusValidator extends TransactionValidator {

    private final AccountRepository accountRepository;

    public AccountStatusValidator(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public boolean validate(Transaction transaction) {
        log.debug("Validating account status for transaction: {}", transaction.getTransactionReference());

        if (transaction.getFromAccountId() != null && !isAccountActive(transaction.getFromAccountId())) {
            transaction.setErrorMessage("Source account is not active: " + transaction.getFromAccountId());
            return false;
        }

        if (transaction.getToAccountId() != null && !isAccountActive(transaction.getToAccountId())) {
            transaction.setErrorMessage("Destination account is not active: " + transaction.getToAccountId());
            return false;
        }

        return validateNext(transaction);
    }

    private boolean isAccountActive(Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        return accountOpt.isPresent() && accountOpt.get().getStatus() == AccountStatus.ACTIVE;
    }
}

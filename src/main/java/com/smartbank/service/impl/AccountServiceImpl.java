package com.smartbank.service.impl;

import com.smartbank.dto.AccountDTO;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.Account;
import com.smartbank.entity.DailyTransactionLimit;
import com.smartbank.enums.AccountStatus;
import com.smartbank.enums.AccountType;
import com.smartbank.exception.AccountNotFoundException;
import com.smartbank.exception.DailyLimitExceededException;
import com.smartbank.repository.AccountRepository;
import com.smartbank.repository.DailyTransactionLimitRepository;
import com.smartbank.service.AccountService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;


@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DailyTransactionLimitRepository limitRepository;

    private static final BigDecimal DAILY_LIMIT = new BigDecimal("50000");

    @Override
    public Account createAccount(AccountDTO accountDTO) {
        Account account = Account.builder()
                .accountNumber(accountDTO.getAccountNumber())
                .customerName(accountDTO.getCustomerName())
                .email(accountDTO.getEmail())
                .mobileNumber(accountDTO.getMobileNumber())
                .balance(accountDTO.getBalance() != null ? accountDTO.getBalance() : BigDecimal.ZERO)
                .accountType(accountDTO.getAccountType() != null
                        ? AccountType.valueOf(accountDTO.getAccountType().toUpperCase())
                        : AccountType.SAVINGS)
                .status(accountDTO.getStatus() != null
                        ? AccountStatus.valueOf(accountDTO.getStatus().toUpperCase())
                        : AccountStatus.ACTIVE)
                .createdDate(LocalDateTime.now())
                .updatedDate(LocalDateTime.now())
                .build();

        return accountRepository.save(account);
    }

    @Override
    public TransactionResponse  deposit(Long accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Only active accounts can receive deposits");
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setUpdatedDate(LocalDateTime.now());

        accountRepository.save(account);

        return TransactionResponse.builder()
                .transactionReference(UUID.randomUUID().toString())
                .status("SUCCESS")
                .amount(amount)
                .balanceAfterTransaction(account.getBalance())
                .transactionDate(LocalDateTime.now())
                .message("Deposit successful")
                .build();
    }

    @Transactional
    public TransactionResponse withdraw(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found"));

        // Validate balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Lock daily transaction limit
        DailyTransactionLimit dailyLimit = limitRepository
                .findByAccountIdAndTransactionDateForUpdate(accountId, LocalDate.now())
                .orElse(DailyTransactionLimit.builder()
                        .accountId(accountId)
                        .transactionDate(LocalDate.now())
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        if (dailyLimit.getTotalAmount().add(amount).compareTo(DAILY_LIMIT) > 0) {
            throw new DailyLimitExceededException("Daily transaction limit exceeded!");
        }

        // Update account balance
        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedDate(LocalDateTime.now());
        accountRepository.save(account);

        // Update daily limit
        dailyLimit.setTotalAmount(dailyLimit.getTotalAmount().add(amount));
        limitRepository.save(dailyLimit);

        // Build response
        return TransactionResponse.builder()
                .transactionReference(UUID.randomUUID().toString())
                .status("SUCCESS")
                .amount(amount)
                .balanceAfterTransaction(account.getBalance())
                .transactionDate(LocalDateTime.now())
                .message("Withdrawal successful")
                .build();
    }

    @Transactional
    public TransactionResponse transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        Account sender = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account receiver = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        // Validate sender balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in sender account");
        }

        // Lock sender's daily limit
        DailyTransactionLimit senderLimit = limitRepository
                .findByAccountIdAndTransactionDateForUpdate(fromAccountId, LocalDate.now())
                .orElse(DailyTransactionLimit.builder()
                        .accountId(fromAccountId)
                        .transactionDate(LocalDate.now())
                        .totalAmount(BigDecimal.ZERO)
                        .build());

        if (senderLimit.getTotalAmount().add(amount).compareTo(DAILY_LIMIT) > 0) {
            throw new DailyLimitExceededException("Daily transaction limit exceeded!");
        }

        // Update balances
        sender.setBalance(sender.getBalance().subtract(amount));
        sender.setUpdatedDate(LocalDateTime.now());
        receiver.setBalance(receiver.getBalance().add(amount));
        receiver.setUpdatedDate(LocalDateTime.now());
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Update sender's daily total
        senderLimit.setTotalAmount(senderLimit.getTotalAmount().add(amount));
        limitRepository.save(senderLimit);

        // Build response
        return TransactionResponse.builder()
                .transactionReference(UUID.randomUUID().toString())
                .status("SUCCESS")
                .amount(amount)
                .balanceAfterTransaction(sender.getBalance())
                .transactionDate(LocalDateTime.now())
                .message("Transfer successful")
                .build();
    }



}

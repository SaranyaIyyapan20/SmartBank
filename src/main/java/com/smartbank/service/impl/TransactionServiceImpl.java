package com.smartbank.service.impl;

import com.smartbank.algorithm.RateLimiter;
import com.smartbank.dto.*;
import com.smartbank.entity.*;
import com.smartbank.enums.AccountStatus;
import com.smartbank.enums.TransactionStatus;
import com.smartbank.enums.TransactionType;
import com.smartbank.exception.*;
import com.smartbank.factory.PaymentStrategyFactory;
import com.smartbank.repository.*;
import com.smartbank.service.TransactionService;
import com.smartbank.strategy.PaymentStrategy;
import com.smartbank.validation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    private final RateLimiter rateLimiter;
    private final ExecutorService executorService;
    private TransactionValidator validatorChain;

    public TransactionServiceImpl() {
        this.rateLimiter = new RateLimiter(100, 1000); // 100 requests per second
        this.executorService = Executors.newFixedThreadPool(10);
    }

    @PostConstruct
    public void init() {
        this.validatorChain = buildValidationChain();
    }

    private TransactionValidator buildValidationChain() {
        TransactionValidator amountValidator = new AmountValidator();
        TransactionValidator balanceValidator = new BalanceValidator(accountRepository);
        TransactionValidator statusValidator = new AccountStatusValidator(accountRepository);
        TransactionValidator dailyLimitValidator = new DailyLimitValidator(transactionRepository);

        amountValidator.setNext(balanceValidator);
        balanceValidator.setNext(statusValidator);
        statusValidator.setNext(dailyLimitValidator);

        return amountValidator;
    }

    // ==================== 1. PROCESS TRANSACTION ====================
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction: {}", request);

        // Rate limiting
        String rateLimitKey = "txn_" + (request.getFromAccountId() != null ?
                request.getFromAccountId() :
                request.getToAccountId());

        if (!rateLimiter.allowRequest(rateLimitKey)) {
            throw new RateLimitExceededException("Too many requests. Please try after some time.");
        }

        // Build transaction using Builder pattern
        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionRef())
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .type(TransactionType.valueOf(request.getTransactionType()))
                .status(TransactionStatus.PENDING)
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .build();

        // Validate using Chain of Responsibility pattern
        if (!validatorChain.validate(transaction)) {
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);

            return TransactionResponse.builder()
                    .transactionReference(transaction.getTransactionReference())
                    .status("FAILED")
                    .message(transaction.getErrorMessage())
                    .amount(transaction.getAmount())
                    .transactionDate(transaction.getTransactionDate())
                    .balanceAfterTransaction(null)
                    .build();
        }

        try {
            // Get strategy using Factory pattern
            PaymentStrategy strategy = paymentStrategyFactory.getStrategy(transaction.getType());
            boolean success = strategy.processPayment(transaction);

            if (success) {
                // Update balances with pessimistic locking
                BigDecimal balanceAfterTxn = updateAccountBalances(transaction);
                transaction.setStatus(TransactionStatus.SUCCESS);

                // Save transaction
                transactionRepository.save(transaction);

                log.info("Transaction completed successfully: {}",
                        transaction.getTransactionReference());

                return TransactionResponse.builder()
                        .transactionReference(transaction.getTransactionReference())
                        .status("SUCCESS")
                        .message("Transaction completed successfully")
                        .amount(transaction.getAmount())
                        .transactionDate(transaction.getTransactionDate())
                        .balanceAfterTransaction(balanceAfterTxn)
                        .build();

            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setErrorMessage("Payment processing failed");
                transactionRepository.save(transaction);

                return TransactionResponse.builder()
                        .transactionReference(transaction.getTransactionReference())
                        .status("FAILED")
                        .message("Payment processing failed")
                        .amount(transaction.getAmount())
                        .transactionDate(transaction.getTransactionDate())
                        .build();
            }

        } catch (Exception e) {
            log.error("Transaction failed: ", e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);

            return TransactionResponse.builder()
                    .transactionReference(transaction.getTransactionReference())
                    .status("FAILED")
                    .message(e.getMessage())
                    .amount(transaction.getAmount())
                    .transactionDate(transaction.getTransactionDate())
                    .build();
        }
    }

    // ==================== 2. DEPOSIT ====================
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse deposit(Long accountId, BigDecimal amount) {
        log.info("Processing deposit: accountId={}, amount={}", accountId, amount);

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + accountId));

        // Validate account status
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(
                    "Account is not active. Current status: " + account.getStatus());
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero");
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionRef())
                .toAccountId(accountId)
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.PENDING)
                .description("Cash deposit")
                .priority(1)
                .build();

        try {
            // Get account with pessimistic lock
            Account lockedAccount = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Account not found with ID: " + accountId));

            // Update balance
            BigDecimal newBalance = lockedAccount.getBalance().add(amount);
            lockedAccount.setBalance(newBalance);
            accountRepository.save(lockedAccount);

            // Update cache
            // Mark transaction as success
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);


            log.info("Deposit successful: {}, New balance: {}",
                    transaction.getTransactionReference(), newBalance);

            return TransactionResponse.builder()
                    .transactionReference(transaction.getTransactionReference())
                    .status("SUCCESS")
                    .message("Deposit successful")
                    .amount(amount)
                    .transactionDate(transaction.getTransactionDate())
                    .balanceAfterTransaction(newBalance)
                    .build();

        } catch (Exception e) {
            log.error("Deposit failed: ", e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);

            throw new InvalidTransactionException("Deposit failed: " + e.getMessage());
        }
    }

    // ==================== 3. WITHDRAWAL ====================
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse withdraw(Long accountId, BigDecimal amount) {
        log.info("Processing withdrawal: accountId={}, amount={}", accountId, amount);

        // Validate account exists
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + accountId));

        // Validate account status
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(
                    "Account is not active. Current status: " + account.getStatus());
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }

        // Check daily limit
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        BigDecimal todaysTotal = transactionRepository.getTotalDebitsSince(accountId, startOfDay);
        if (todaysTotal == null) {
            todaysTotal = BigDecimal.ZERO;
        }

        BigDecimal dailyLimit = new BigDecimal("500000.00");
        if (todaysTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new DailyLimitExceededException(
                    String.format("Daily withdrawal limit exceeded. Limit: %s, Today's total: %s",
                            dailyLimit, todaysTotal));
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionRef())
                .fromAccountId(accountId)
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.PENDING)
                .description("Cash withdrawal")
                .priority(1)
                .build();

        try {
            // Get account with pessimistic lock
            Account lockedAccount = accountRepository.findByIdWithLock(accountId)
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Account not found with ID: " + accountId));

            // Check sufficient balance
            if (lockedAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance. Available: %s, Required: %s",
                                lockedAccount.getBalance(), amount));
            }

            // Update balance
            BigDecimal newBalance = lockedAccount.getBalance().subtract(amount);
            lockedAccount.setBalance(newBalance);
            accountRepository.save(lockedAccount);


            // Mark transaction as success
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);

            log.info("Withdrawal successful: {}, New balance: {}",
                    transaction.getTransactionReference(), newBalance);

            return TransactionResponse.builder()
                    .transactionReference(transaction.getTransactionReference())
                    .status("SUCCESS")
                    .message("Withdrawal successful")
                    .amount(amount)
                    .transactionDate(transaction.getTransactionDate())
                    .balanceAfterTransaction(newBalance)
                    .build();

        } catch (InsufficientBalanceException | DailyLimitExceededException e) {
            log.error("Withdrawal failed: ", e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);
            throw e;

        } catch (Exception e) {
            log.error("Withdrawal failed: ", e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);

            throw new InvalidTransactionException("Withdrawal failed: " + e.getMessage());
        }
    }

    // ==================== 4. TRANSFER ====================
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.info("Processing transfer: from={}, to={}, amount={}",
                fromAccountId, toAccountId, amount);

        // Validate accounts are different
        if (fromAccountId.equals(toAccountId)) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        // Validate both accounts exist
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Source account not found with ID: " + fromAccountId));

        Account toAccount = accountRepository.findById(toAccountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Destination account not found with ID: " + toAccountId));

        // Validate both accounts are active
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(
                    "Source account is not active. Current status: " + fromAccount.getStatus());
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(
                    "Destination account is not active. Current status: " + toAccount.getStatus());
        }

        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        // Check daily limit
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        BigDecimal todaysTotal = transactionRepository.getTotalDebitsSince(fromAccountId, startOfDay);
        if (todaysTotal == null) {
            todaysTotal = BigDecimal.ZERO;
        }

        BigDecimal dailyLimit = new BigDecimal("500000.00");
        if (todaysTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new DailyLimitExceededException(
                    String.format("Daily transfer limit exceeded. Limit: %s, Today's total: %s",
                            dailyLimit, todaysTotal));
        }

        // Create transaction
        Transaction transaction = Transaction.builder()
                .transactionReference(generateTransactionRef())
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.PENDING)
                .description(String.format("Transfer from %s to %s",
                        fromAccount.getAccountNumber(), toAccount.getAccountNumber()))
                .priority(2)
                .build();

        try {
            // Get both accounts with pessimistic lock (lock in order to avoid deadlock)
            Long firstLockId = Math.min(fromAccountId, toAccountId);
            Long secondLockId = Math.max(fromAccountId, toAccountId);

            Account firstAccount = accountRepository.findByIdWithLock(firstLockId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + firstLockId));

            Account secondAccount = accountRepository.findByIdWithLock(secondLockId)
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + secondLockId));

            // Determine which is source and destination
            Account lockedFromAccount = firstLockId.equals(fromAccountId) ? firstAccount : secondAccount;
            Account lockedToAccount = firstLockId.equals(toAccountId) ? firstAccount : secondAccount;

            // Check sufficient balance
            if (lockedFromAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        String.format("Insufficient balance. Available: %s, Required: %s",
                                lockedFromAccount.getBalance(), amount));
            }

            // Update balances
            BigDecimal newFromBalance = lockedFromAccount.getBalance().subtract(amount);
            BigDecimal newToBalance = lockedToAccount.getBalance().add(amount);

            lockedFromAccount.setBalance(newFromBalance);
            lockedToAccount.setBalance(newToBalance);

            accountRepository.save(lockedFromAccount);
            accountRepository.save(lockedToAccount);


            // Mark transaction as success
            transaction.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(transaction);


            log.info("Transfer successful: {}, From balance: {}, To balance: {}",
                    transaction.getTransactionReference(), newFromBalance, newToBalance);

            return TransactionResponse.builder()
                    .transactionReference(transaction.getTransactionReference())
                    .status("SUCCESS")
                    .message(String.format("Transfer successful to %s", toAccount.getAccountNumber()))
                    .amount(amount)
                    .transactionDate(transaction.getTransactionDate())
                    .balanceAfterTransaction(newFromBalance)
                    .build();

        } catch (InsufficientBalanceException | DailyLimitExceededException e) {
            log.error("Transfer failed: ", e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);
            throw e;

        } catch (Exception e) {
            log.error("Transfer failed: ", e);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setErrorMessage(e.getMessage());
            transactionRepository.save(transaction);

            throw new InvalidTransactionException("Transfer failed: " + e.getMessage());
        }
    }

    // ==================== 5. GET TRANSACTION HISTORY ====================
    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(Long accountId,
                                                   LocalDateTime startDate,
                                                   LocalDateTime endDate) {
        log.info("Fetching transaction history: accountId={}, start={}, end={}",
                accountId, startDate, endDate);

        // Validate account exists
        accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found with ID: " + accountId));

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new InvalidTransactionException("Start date cannot be after end date");
        }

        // Fetch transactions
        List<Transaction> transactions = transactionRepository
                .findAccountTransactions(accountId, startDate, endDate);

        log.info("Found {} transactions for account {}", transactions.size(), accountId);

        return transactions;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Update account balances for a transaction
     * Uses pessimistic locking to prevent concurrent modification
     */
    private BigDecimal updateAccountBalances(Transaction transaction) {
        BigDecimal balanceAfterTransaction = null;

        // Debit from source account
        if (transaction.getFromAccountId() != null) {
            Account fromAccount = accountRepository.findByIdWithLock(transaction.getFromAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Source account not found: " + transaction.getFromAccountId()));

            BigDecimal newBalance = fromAccount.getBalance().subtract(transaction.getAmount());
            fromAccount.setBalance(newBalance);
            accountRepository.save(fromAccount);

            balanceAfterTransaction = newBalance;
        }

        // Credit to destination account
        if (transaction.getToAccountId() != null) {
            Account toAccount = accountRepository.findByIdWithLock(transaction.getToAccountId())
                    .orElseThrow(() -> new AccountNotFoundException(
                            "Destination account not found: " + transaction.getToAccountId()));

            BigDecimal newBalance = toAccount.getBalance().add(transaction.getAmount());
            toAccount.setBalance(newBalance);
            accountRepository.save(toAccount);

            // If only credit transaction (deposit), return this balance
            if (transaction.getFromAccountId() == null) {
                balanceAfterTransaction = newBalance;
            }
        }

        return balanceAfterTransaction;
    }

    /**
     * Generate unique transaction reference
     * Format: TXN + timestamp + random UUID
     */
    private String generateTransactionRef() {
        return "TXN" + System.currentTimeMillis() +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Shutdown executor service gracefully
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
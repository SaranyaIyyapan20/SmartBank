package com.smartbank.controller;

import com.smartbank.dto.*;
import com.smartbank.entity.Transaction;
import com.smartbank.service.AccountService;
import com.smartbank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction Management", description = "Complete Transaction APIs")
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    // ==================== 1. GENERIC TRANSACTION API ====================

    /**
     * Process any type of transaction (DEPOSIT, WITHDRAWAL, TRANSFER, BILL_PAYMENT, etc.)
     * POST /api/v1/transactions
     */
    @PostMapping
    @Operation(summary = "Process Transaction",
            description = "Generic API to process any type of transaction")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {

        log.info("REST API: Process transaction - Type: {}, Amount: {}",
                request.getTransactionType(), request.getAmount());

        TransactionResponse response = transactionService.processTransaction(request);

        HttpStatus status = "SUCCESS".equals(response.getStatus()) ?
                HttpStatus.OK : HttpStatus.BAD_REQUEST;

        return new ResponseEntity<>(response, status);
    }


    // ==================== 2. DEPOSIT APIs ====================

    /**
     * Deposit money into an account - Simple version
     * POST /api/v1/transactions/deposit?accountId=1&amount=5000
     */
    @PostMapping("/deposit")
    @Operation(summary = "Deposit Money",
            description = "Deposit money into an account")
    public ResponseEntity<TransactionResponse> deposit(
            @RequestParam Long accountId,
            @RequestParam BigDecimal amount) {

        log.info("REST API: Deposit - AccountId: {}, Amount: {}", accountId, amount);

        TransactionResponse response = transactionService.deposit(accountId, amount);
        return ResponseEntity.ok(response);
    }



    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Deposit Money", description = "Deposit money into account")
    public ResponseEntity<Map<String, Object>> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request) {

        log.info("REST API: Deposit - AccountId: {}, Amount: {}", accountId, request.getAmount());

        TransactionResponse txn = accountService.deposit(accountId, request.getAmount());

        return ResponseEntity.ok(Map.of(
                "transactionReference", txn.getTransactionReference(),
                "status", txn.getStatus(),
                "amount", txn.getAmount(),
                "balanceAfterTransaction", txn.getBalanceAfterTransaction(),
                "message", txn.getMessage()
        ));
    }

    @PostMapping("/{accountId}/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @PathVariable Long accountId,
            @RequestBody Map<String, BigDecimal> request) {

        BigDecimal amount = request.get("amount");
        TransactionResponse txn = accountService.withdraw(accountId, amount);

        return  ResponseEntity.ok(Map.of(
                "transactionReference", txn.getTransactionReference(),
                "status", txn.getStatus(),
                "amount", txn.getAmount(),
                "balanceAfterTransaction", txn.getBalanceAfterTransaction(),
                "message", txn.getMessage()
        ));
    }

    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(@RequestBody Map<String, Object> request) {
        Long fromAccountId = Long.valueOf(request.get("fromAccountId").toString());
        Long toAccountId = Long.valueOf(request.get("toAccountId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());

        accountService.transfer(fromAccountId, toAccountId, amount);
        return ResponseEntity.ok("Transfer successful from account " + fromAccountId + " to " + toAccountId);
    }

    // ==================== 4. TRANSFER APIs ====================

    /**
     * Transfer money between accounts - Simple version
     * POST /api/v1/transactions/transfer?fromAccountId=1&toAccountId=2&amount=5000
     */
   /* @PostMapping("/transfer")
    @Operation(summary = "Transfer Money",
            description = "Transfer money between two accounts")
    public ResponseEntity<TransactionResponse> transfer(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam BigDecimal amount) {

        log.info("REST API: Transfer - From: {}, To: {}, Amount: {}",
                fromAccountId, toAccountId, amount);

        TransactionResponse response = transactionService.transfer(
                fromAccountId, toAccountId, amount
        );

        return ResponseEntity.ok(response);
    }*/

    /**
     * Transfer money - Detailed version
     * POST /api/v1/transactions/transfer/detailed

    // ==================== 6. TRANSACTION HISTORY APIs ====================

    /**
     * Get transaction history for an account with date range
     * GET /api/v1/transactions/history/1?startDate=2025-01-01T00:00:00&endDate=2025-01-31T23:59:59
     */
    @GetMapping("/history/{accountId}")
    @Operation(summary = "Get Transaction History",
            description = "Get transaction history for a specific date range")
    public ResponseEntity<List<Transaction>> getTransactionHistory(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("REST API: Transaction history - AccountId: {}, Start: {}, End: {}",
                accountId, startDate, endDate);

        List<Transaction> transactions = transactionService.getTransactionHistory(
                accountId, startDate, endDate
        );

        return ResponseEntity.ok(transactions);
    }

    /**
     * Get recent transactions (last 30 days)
     * GET /api/v1/transactions/history/1/recent
     */
    @GetMapping("/history/{accountId}/recent")
    @Operation(summary = "Get Recent Transactions",
            description = "Get last 30 days transactions")
    public ResponseEntity<List<Transaction>> getRecentTransactions(
            @PathVariable Long accountId) {

        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(30);

        log.info("REST API: Recent transactions - AccountId: {}", accountId);

        List<Transaction> transactions = transactionService.getTransactionHistory(
                accountId, startDate, endDate
        );

        return ResponseEntity.ok(transactions);
    }

    /**
     * Get today's transactions
     * GET /api/v1/transactions/history/1/today
     */
    @GetMapping("/history/{accountId}/today")
    @Operation(summary = "Get Today's Transactions",
            description = "Get all transactions for today")
    public ResponseEntity<List<Transaction>> getTodaysTransactions(
            @PathVariable Long accountId) {

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);

        log.info("REST API: Today's transactions - AccountId: {}", accountId);

        List<Transaction> transactions = transactionService.getTransactionHistory(
                accountId, startOfDay, endOfDay
        );

        return ResponseEntity.ok(transactions);
    }


}
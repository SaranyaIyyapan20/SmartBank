package com.smartbank.service;

import com.smartbank.dto.TransactionRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.Transaction;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {

    TransactionResponse processTransaction(TransactionRequest request);

    TransactionResponse deposit(Long accountId, java.math.BigDecimal amount);

    TransactionResponse withdraw(Long accountId, java.math.BigDecimal amount);

    TransactionResponse transfer(Long fromId, Long toId, java.math.BigDecimal amount);

    List<Transaction> getTransactionHistory(Long accountId, LocalDateTime start, LocalDateTime end);
}

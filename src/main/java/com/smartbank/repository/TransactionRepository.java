package com.smartbank.repository;

import com.smartbank.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccountIdOrToAccountId(Long fromAccountId, Long toAccountId);

    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccountId = :accountId OR t.toAccountId = :accountId) " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findAccountTransactions(
            Long accountId, LocalDateTime startDate, LocalDateTime endDate
    );

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE " +
            "t.fromAccountId = :accountId AND t.status = 'SUCCESS' " +
            "AND t.transactionDate >= :startDate")
    BigDecimal getTotalDebitsSince(Long accountId, LocalDateTime startDate);
}

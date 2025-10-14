package com.smartbank.repository;

import com.smartbank.entity.DailyTransactionLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTransactionLimitRepository extends JpaRepository<DailyTransactionLimit, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyTransactionLimit d WHERE d.accountId = :accountId AND d.transactionDate = :date")
    Optional<DailyTransactionLimit> findByAccountIdAndTransactionDateForUpdate(
            @Param("accountId") Long accountId,
            @Param("date") LocalDate date);

    Optional<DailyTransactionLimit> findByAccountIdAndTransactionDate(Long accountId, LocalDate date);
}


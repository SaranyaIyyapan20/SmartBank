package com.smartbank.repository;

import com.smartbank.entity.Account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByCustomerId(Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountId = :accountId")
    Optional<Account> findByIdWithLock(Long accountId);

    @Query("SELECT a FROM Account a WHERE a.status = 'ACTIVE' AND a.balance > :minBalance")
    List<Account> findActiveAccountsWithMinBalance(BigDecimal minBalance);
}

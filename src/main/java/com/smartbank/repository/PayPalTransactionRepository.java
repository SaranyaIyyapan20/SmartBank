package com.smartbank.repository;

import com.smartbank.entity.PayPalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayPalTransactionRepository extends JpaRepository<PayPalTransaction, String> {
}


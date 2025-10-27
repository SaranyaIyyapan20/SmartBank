package com.smartbank.repository;

import com.smartbank.entity.PayPalUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayPalUserRepository extends JpaRepository<PayPalUser, Long> {
}

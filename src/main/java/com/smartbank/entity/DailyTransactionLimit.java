package com.smartbank.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_transaction_limit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long accountId;

    private LocalDate transactionDate;

    private BigDecimal totalAmount;
}

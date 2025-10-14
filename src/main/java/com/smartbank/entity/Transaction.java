package com.smartbank.entity;

import com.smartbank.enums.TransactionStatus;
import com.smartbank.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_ref", columnList = "transactionReference"),
        @Index(name = "idx_from_account", columnList = "fromAccountId"),
        @Index(name = "idx_to_account", columnList = "toAccountId"),
        @Index(name = "idx_txn_date", columnList = "transactionDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false, unique = true, length = 50)
    private String transactionReference;

    @Column(nullable = false)
    private Long fromAccountId;

    private Long toAccountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(length = 500)
    private String errorMessage;

    private Integer priority; // For priority queue

    @PrePersist
    protected void onCreate() {
        transactionDate = LocalDateTime.now();
    }
}

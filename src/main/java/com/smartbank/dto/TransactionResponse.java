package com.smartbank.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private String transactionReference;
    private String status;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private String message;
    private BigDecimal balanceAfterTransaction;
}

package com.smartbank.dto;

import lombok.Data;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    private Long fromAccountId;

    private Long toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    private String transactionType;

    @Size(max = 500, message = "Description too long")
    private String description;

    private Integer priority; // For priority queue
}
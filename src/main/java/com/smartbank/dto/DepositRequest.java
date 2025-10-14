package com.smartbank.dto;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Data
public class DepositRequest {

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "0.01", message = "Deposit amount must be greater than zero")
    private BigDecimal amount;

    private String remarks;
}

package com.smartbank.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BalanceResponse {
    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime lastUpdated;
}

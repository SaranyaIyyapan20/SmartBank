package com.smartbank.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    private Long senderId;
    private Long receiverId;
    private BigDecimal amount;
    private String transactionId;
}


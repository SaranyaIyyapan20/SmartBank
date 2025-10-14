package com.smartbank.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountDTO {
    private String accountNumber;
    private String customerName;
    private String email;
    private String mobileNumber;
    private BigDecimal balance;
    private String accountType;
    private String status;
}
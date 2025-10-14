package com.smartbank.dto;

import lombok.Data;

@Data
public class Beneficiary {
    private Long id;
    private String beneficiaryName;
    private String accountNumber;
}
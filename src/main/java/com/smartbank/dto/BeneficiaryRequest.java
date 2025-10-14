package com.smartbank.dto;

import lombok.Data;

@Data
public class BeneficiaryRequest {
    private String beneficiaryName;
    private String accountNumber;
}

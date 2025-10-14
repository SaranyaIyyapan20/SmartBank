package com.smartbank.dto;

import lombok.Data;

@Data
public class AccountUpdateRequest {
    private String customerName;
    private String email;
    private String mobileNumber;
    private String accountType;
}

package com.smartbank.dto;

import lombok.Data;

@Data
public class AccountVerificationResponse {
    private boolean exists;
    private String message;
}
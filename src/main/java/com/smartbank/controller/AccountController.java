package com.smartbank.controller;

import com.smartbank.dto.AccountDTO;
import com.smartbank.dto.DepositRequest;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.Account;
import com.smartbank.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RestControllerAdvice
@RequestMapping("/api/v1/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody AccountDTO accountDTO) {
        Account account = accountService.createAccount(accountDTO);
        return new ResponseEntity<>(account, HttpStatus.CREATED);
    }



}

package com.smartbank.service;

import com.smartbank.dto.AccountDTO;
import com.smartbank.dto.TransactionResponse;
import com.smartbank.entity.Account;
import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    public Account createAccount(AccountDTO accountDTO);

    public TransactionResponse  deposit(Long accountId, BigDecimal amount);

    public TransactionResponse withdraw(Long accountId, BigDecimal amount);

    public TransactionResponse transfer(Long fromAccountId, Long toAccountId, BigDecimal amount);

}

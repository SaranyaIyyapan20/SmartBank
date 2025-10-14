package com.smartbank.factory;

import com.smartbank.enums.TransactionType;
import com.smartbank.strategy.PaymentStrategy;
import com.smartbank.strategy.impl.*;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    private final com.smartbank.strategy.impl.DepositStrategy depositStrategy = new DepositStrategy();
    private final WithdrawalStrategy withdrawalStrategy = new WithdrawalStrategy();
    private final TransferStrategy transferStrategy = new TransferStrategy();

    public PaymentStrategy getStrategy(TransactionType type) {
        switch (type) {
            case DEPOSIT: return depositStrategy;
            case WITHDRAWAL: return withdrawalStrategy;
            case TRANSFER: return transferStrategy;
            default: throw new IllegalArgumentException("Unknown transaction type: " + type);
        }
    }
}

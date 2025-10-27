package com.smartbank.service.impl;


import com.smartbank.entity.PayPalTransaction;
import com.smartbank.entity.PayPalUser;
import com.smartbank.exception.InsufficientBalanceException;
import com.smartbank.exception.InvalidUser;
import com.smartbank.repository.PayPalTransactionRepository;
import com.smartbank.repository.PayPalUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class PaymentService {

    private final PayPalUserRepository userRepository;
    private final PayPalTransactionRepository transactionRepository;

    public PaymentService(PayPalUserRepository userRepository, PayPalTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public String transferMoney(String transactionId, Long senderId, Long receiverId, BigDecimal amount) {
        // Idempotency check
        if (transactionRepository.existsById(transactionId)) {
            return "Transaction already processed: " + transactionId;
        }


        PayPalUser sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new InvalidUser("Sender not found"));
        PayPalUser receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new InvalidUser("Receiver not found"));
        // Add validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InsufficientBalanceException("Amount must be positive");
        }
        if (senderId.equals(receiverId)) {
            throw new InsufficientBalanceException("Cannot transfer to yourself");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        userRepository.save(sender);
        userRepository.save(receiver);

        PayPalTransaction txn = new PayPalTransaction();
        txn.setTransactionId(transactionId);
        txn.setSenderId(senderId);
        txn.setReceiverId(receiverId);
        txn.setAmount(amount);
        txn.setStatus(PayPalTransaction.Status.SUCCESS);

        transactionRepository.save(txn);

        return "Transaction successful: " + transactionId;
    }
}


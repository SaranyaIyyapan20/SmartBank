package com.smartbank.controller;

import com.smartbank.dto.TransferRequest;
import com.smartbank.service.impl.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request) {

        Long senderId = request.getSenderId();
        Long receiverId = request.getReceiverId();
        BigDecimal amount = request.getAmount();
        String transactionId = request.getTransactionId();

        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = UUID.randomUUID().toString(); // ensure idempotency key
        }

        return paymentService.transferMoney(transactionId, senderId, receiverId, amount);
    }
}


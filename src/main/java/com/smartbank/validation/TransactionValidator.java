package com.smartbank.validation;

import com.smartbank.entity.Transaction;

public abstract class TransactionValidator {
    protected TransactionValidator next;

    public void setNext(TransactionValidator validator) {
        this.next = validator;
    }

    public abstract boolean validate(Transaction transaction);

    protected boolean validateNext(Transaction transaction) {
        if (next != null) {
            return next.validate(transaction);
        }
        return true;
    }
}

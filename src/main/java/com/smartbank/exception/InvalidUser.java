package com.smartbank.exception;

public class InvalidUser extends RuntimeException{
    public InvalidUser(String message) {
        super(message);
    }
}

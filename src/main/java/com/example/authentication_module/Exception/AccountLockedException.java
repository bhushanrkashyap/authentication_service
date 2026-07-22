package com.example.authentication_module.Exception;

public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super("Account locked for user: " + message);
    }
}

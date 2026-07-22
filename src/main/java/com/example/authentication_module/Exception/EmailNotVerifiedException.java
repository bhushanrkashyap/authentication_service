package com.example.authentication_module.Exception;

public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String message) {
        super("Account not verified: " + message);
    }
}

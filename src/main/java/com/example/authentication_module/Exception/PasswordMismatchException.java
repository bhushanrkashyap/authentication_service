package com.example.authentication_module.Exception;

public class PasswordMismatchException extends RuntimeException {
    public PasswordMismatchException(String message) {
        super("Invalid password for user: " + message);
    }
}

package com.example.authentication_module.Exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super("Invalid credentials for user: " + message);
    }
}

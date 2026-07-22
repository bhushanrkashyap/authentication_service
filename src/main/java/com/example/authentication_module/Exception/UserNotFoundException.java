package com.example.authentication_module.Exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super("User not found: " + message);
    }
}

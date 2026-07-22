package com.example.authentication_module.Exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super("Resource not found for user: " + message);
    }
}

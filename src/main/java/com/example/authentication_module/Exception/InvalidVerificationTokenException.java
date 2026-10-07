package com.example.authentication_module.Exception;

/**
 * Thrown when an email verification token does not exist or has already been
 * used. Maps to HTTP 400 so the frontend can show an "Invalid verification
 * link" message.
 */
public class InvalidVerificationTokenException extends RuntimeException {
    public InvalidVerificationTokenException(String message) {
        super(message);
    }
}

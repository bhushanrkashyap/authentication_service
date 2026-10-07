package com.example.authentication_module.Exception;

/**
 * Thrown when an email verification token still exists in the database but its
 * expiry date has passed. Maps to HTTP 410 (Gone) so the frontend can show an
 * "expired" message.
 */
public class ExpiredVerificationTokenException extends RuntimeException {
    public ExpiredVerificationTokenException(String message) {
        super(message);
    }
}

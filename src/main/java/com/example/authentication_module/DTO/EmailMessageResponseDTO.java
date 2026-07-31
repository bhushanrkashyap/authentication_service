package com.example.authentication_module.DTO;

import java.time.LocalDateTime;

public class EmailMessageResponseDTO {

        private String message;
        private boolean success;
        private LocalDateTime timestamp;

        public EmailMessageResponseDTO()
        {}

        public EmailMessageResponseDTO(String message, boolean success) {
            this.message = message;
            this.success = success;
            this.timestamp = LocalDateTime.now();
        }

    public EmailMessageResponseDTO(String message, boolean success, LocalDateTime timestamp) {
        this.message = message;
        this.success = success;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

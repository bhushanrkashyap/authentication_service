package com.example.authentication_module.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

 public class EmailAlreadyexistsException extends RuntimeException {

        public EmailAlreadyexistsException(String message) {
            super(message);
        }
    }


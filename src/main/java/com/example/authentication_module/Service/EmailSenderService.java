package com.example.authentication_module.Service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService{

    private final JavaMailSender mailSender;

    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(
            String to,
            String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Verify your email");

        message.setText(
                "Click the link to verify your account:\n\n" +
                        "http://localhost:8080/auth/verify-email?token=" + token
        );

        mailSender.send(message);
    }
    public void sendPasswordResetEmail(String email, String token) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);
        message.setSubject("Reset Your Password");

        message.setText(
                "Click the link below to reset your password:\n\n"
                        + "http://localhost:8080/auth/reset-password?token=" + token
        );

        mailSender.send(message);
    }
}

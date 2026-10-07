package com.example.authentication_module.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService{

        private static final Logger log = LoggerFactory.getLogger(EmailSenderService.class);

        private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public EmailSenderService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendVerificationEmail(
            String to,
            String token) {

        log.debug("Preparing verification email to: {}", to);
        SimpleMailMessage message = new SimpleMailMessage();

        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }

                String verificationUrl = frontendBaseUrl + "/verify-email?token=" + token;
        log.debug("Verification URL: {}", verificationUrl);

        message.setTo(to);
        message.setSubject("Verify your email");

        message.setText(
                "Click the link to verify your account:\n\n" + verificationUrl
        );

        try {
            mailSender.send(message);
            log.info("Verification email sent to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage(), e);
            throw e;
        } catch (Exception ex) {
            log.error("Unexpected error while sending verification email to {}: {}", to, ex.getMessage(), ex);
            throw ex;
        }
    }
    public void sendPasswordResetEmail(String email, String token) {
        log.debug("Preparing password reset email to: {}", email);
        SimpleMailMessage message = new SimpleMailMessage();

        if (fromAddress != null && !fromAddress.isBlank()) {
            message.setFrom(fromAddress);
        }

        message.setTo(email);
        message.setSubject("Reset Your Password");

        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token;
        message.setText(
                "Click the link below to reset your password:\n\n" + resetUrl
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
            throw e;
        } catch (Exception ex) {
            log.error("Unexpected error while sending password reset email to {}: {}", email, ex.getMessage(), ex);
            throw ex;
        }
    }
}

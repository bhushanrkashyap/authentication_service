package com.example.authentication_module.Repository;

import com.example.authentication_module.model.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationTokenEntity , Long> {
    Optional <EmailVerificationTokenEntity> findByToken(String token);
}

package com.example.authentication_module.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class EmailVerificationTokenEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private String token;

        @OneToOne
        @JoinColumn(name = "user_id")
        private UserModel user;

        private LocalDateTime expiryDate;

        private boolean used;

        public EmailVerificationTokenEntity()
        {

        }
    public EmailVerificationTokenEntity(Long id, String token, UserModel user, LocalDateTime expiryDate, boolean used) {
        this.id = id;
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;
        this.used = used;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserModel getUser() {
        return user;
    }

    public void setUser(UserModel user) {
        this.user = user;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}


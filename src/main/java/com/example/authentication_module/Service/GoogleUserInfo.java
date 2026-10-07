package com.example.authentication_module.Service;

public record GoogleUserInfo(String subject, String email, boolean emailVerified, String name) {
}

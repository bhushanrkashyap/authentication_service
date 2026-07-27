package com.example.authentication_module.DTO;

import com.example.authentication_module.model.Role;

public class LoginResponseDTO {

    private String username;
    private String email;
    private Role.Roles role;

    private String token;
    private String tokenType;

    public LoginResponseDTO() {
    }

    public LoginResponseDTO(String username,
                            String email,
                            Role.Roles role,
                            String token,
                            String tokenType) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.token = token;
        this.tokenType = tokenType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role.Roles getRole() {
        return role;
    }

    public void setRole(Role.Roles role) {
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
package com.example.authentication_module.DTO;

import com.example.authentication_module.model.Role;

public class LoginResponseDTO {
    private String username;
    private String email;

    public LoginResponseDTO(String username, String email) {
        this.username = username;
        this.email = email;
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

    public Role.Roles setRole(Role.Roles role) {
        return role;
    }
}

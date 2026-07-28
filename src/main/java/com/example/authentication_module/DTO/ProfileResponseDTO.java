package com.example.authentication_module.DTO;

import com.example.authentication_module.model.Role;

public class ProfileResponseDTO {

    private String username;
    private String email;
    private Role.Roles role;

    public ProfileResponseDTO(String username, String email, Role.Roles role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role.Roles getRole() {
        return role;
    }
}
package com.example.authentication_module.DTO;

import com.example.authentication_module.model.Role;
import java.time.LocalDateTime;

public class RegisterResponseDTO {

    private Integer id;
    private String username;
    private String email;
    private Role.Roles role;
    private LocalDateTime registeredAt;

    public RegisterResponseDTO() {
    }

    public RegisterResponseDTO(Integer id,
                               String username,
                               String email,
                               Role.Roles role,
                               LocalDateTime registeredAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.registeredAt = registeredAt;}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }


}
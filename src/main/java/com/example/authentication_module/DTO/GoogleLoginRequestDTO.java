package com.example.authentication_module.DTO;

import jakarta.validation.constraints.NotBlank;

public class GoogleLoginRequestDTO {

    @NotBlank(message = "idToken is required")
    private String idToken;

    public GoogleLoginRequestDTO() {
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

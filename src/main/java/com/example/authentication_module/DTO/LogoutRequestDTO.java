package com.example.authentication_module.DTO;

public class LogoutRequestDTO {
    private String refreshToken;

    public LogoutRequestDTO()
    {

    }
    public LogoutRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}


package com.example.authentication_module.DTO;


public class RefreshTokenRequestDTO {
    private String refreshToken;
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public RefreshTokenRequestDTO()
    {

    }
    public RefreshTokenRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;

    }

}

package com.example.authentication_module.DTO;

public class RefreshTokenResponseDTO {
    private String accessToken;
    private Long expiresIn;

    public RefreshTokenResponseDTO()
    {

    }
    public RefreshTokenResponseDTO(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
}

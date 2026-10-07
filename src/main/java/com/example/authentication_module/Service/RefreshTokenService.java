package com.example.authentication_module.Service;

import com.example.authentication_module.DTO.RefreshTokenRequestDTO;
import com.example.authentication_module.DTO.RefreshTokenResponseDTO;
import com.example.authentication_module.Exception.InvalidCredentialsException;
import com.example.authentication_module.Repository.RefreshTokenRepository;
import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.model.RefreshToken;
import com.example.authentication_module.model.UserModel;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository repo, JwtService jwtService) {
        this.repo = repo;
        this.jwtService = jwtService;
    }

    public RefreshToken createrefreshToken(UserModel user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken.setUser(user);

        refreshToken.setCreatedAt(LocalDateTime.now());

        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));

        refreshToken.setRevoked(false);

        return repo.save(refreshToken);
    }

    public RefreshTokenResponseDTO refreshToken(String refreshTokenValue) {

        RefreshToken refreshToken = repo
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        UserModel user = refreshToken.getUser();

        String accessToken = jwtService.generateToken(user);

        RefreshTokenResponseDTO response = new RefreshTokenResponseDTO();
        response.setAccessToken(accessToken);
        response.setExpiresIn(jwtService.getAccessTokenExpiryInSeconds());

        return response;
    }
}

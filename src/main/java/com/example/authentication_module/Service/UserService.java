package com.example.authentication_module.Service;


import com.example.authentication_module.DTO.*;
import com.example.authentication_module.Exception.*;
import com.example.authentication_module.Repository.RefreshTokenRepository;
import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.mapper.UserMapper;
import com.example.authentication_module.model.RefreshToken;
import com.example.authentication_module.model.Role;
import com.example.authentication_module.model.UserModel;
import io.jsonwebtoken.Jwt;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshRepo;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder , JwtService jwtService , RefreshTokenService refreshTokenService , RefreshTokenRepository refreshRepo) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshRepo = refreshRepo;
    }

    public RegisterResponseDTO registerUser(RegisterRequestDTO requestDTO) {
        if (repo.existsByEmail(requestDTO.getEmail())) {
            throw new EmailAlreadyexistsException("Email already exists!");
        }
        if (!requestDTO.getPassword().equals(requestDTO.getConfirmPassword())) {
            throw new PasswordMismatchException("Password Mismatch!");
        }
        if (repo.existsByPhoneNumber((requestDTO.getPhoneNumber()))) {
            throw new PhoneNumberAlreadyExistsException("Phone number already exists!");
        }

        UserModel user = UserMapper.toEntity(requestDTO);
        user.setPassword(passwordEncoder.encode(requestDTO.getPassword()));
        user.setRole(Role.Roles.USER);
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setEmailVerified(false);

        UserModel savedUser = repo.save(user);
        return UserMapper.toResponse(savedUser);
    }

    public LoginResponseDTO loginUser(LoginRequestDTO requestDTO) {

        UserModel user = repo.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new PasswordMismatchException("Password mismatch!");
        }

        if (!user.isEnabled()) {
            throw new AccountLockedException("Account is disabled!");
        }

        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException("Account is locked!");
        }

        if (!user.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException("Credentials expired!");
        }

        if (!user.isAccountNonExpired()) {
            throw new AccountExpiredException("Account expired!");
        }

        LocalDateTime now = LocalDateTime.now();

        user.setLoginTime(now);
        user.setLastAccessedAt(now);

        repo.save(user);

        String accessToken = jwtService.generateToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createrefreshToken(user);

        LoginResponseDTO responseDTO = new LoginResponseDTO();

        responseDTO.setAccessToken(accessToken);
        responseDTO.setRefreshToken(refreshToken.getToken());
        responseDTO.setExpiresIn(900);

        return responseDTO;
    }


    public ProfileResponseDTO updateProfile(String email,
                                            UpdateProfileRequestDTO dto) {

        System.out.println("1. Service started");

        UserModel user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("2. User found");

        user.setUsername(dto.getUsername());
        System.out.println("3. Username updated");

        user.setPhoneNumber(dto.getPhoneNumber());
        user.setCity(dto.getCity());
        user.setState(dto.getState());
        user.setCountry(dto.getCountry());
        user.setAddress(dto.getAddress());

        System.out.println("4. Saving...");

        UserModel updatedUser = repo.save(user);

        System.out.println("5. Saved");

        return new ProfileResponseDTO(
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                updatedUser.getRole()
        );
    }


    public MessageResponseDTO changePassword(
            String email,
            ChangePasswordRequestDTO request) {

        UserModel user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword())) {

            throw new RuntimeException("Current password is incorrect");
        }

        if (!request.getNewPassword()
                .equals(request.getConfirmNewPassword())) {

            throw new RuntimeException("Passwords do not match");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword()));

        repo.save(user);

        return new MessageResponseDTO("Password changed successfully");
    }



    @Transactional
    public void logout(String refreshTokenValue) {

        RefreshToken refreshToken = refreshRepo
                .findByToken(refreshTokenValue)
                .orElseThrow(() ->
                        new RuntimeException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new RuntimeException("Already logged out");
        }

        refreshToken.setRevoked(true);

        refreshRepo.save(refreshToken);
    }
}





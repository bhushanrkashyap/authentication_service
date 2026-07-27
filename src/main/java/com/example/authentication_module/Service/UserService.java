package com.example.authentication_module.Service;


import com.example.authentication_module.DTO.LoginRequestDTO;
import com.example.authentication_module.DTO.LoginResponseDTO;
import com.example.authentication_module.DTO.RegisterRequestDTO;
import com.example.authentication_module.DTO.RegisterResponseDTO;
import com.example.authentication_module.Exception.*;
import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.mapper.UserMapper;
import com.example.authentication_module.model.Role;
import com.example.authentication_module.model.UserModel;
import io.jsonwebtoken.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
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

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder , JwtService jwtService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

        String token = jwtService.generateToken(user);

        LoginResponseDTO responseDTO = new LoginResponseDTO();
        responseDTO.setUsername(user.getUsername());
        responseDTO.setEmail(user.getEmail());
        responseDTO.setRole(user.getRole());
        responseDTO.setToken(token);
        responseDTO.setTokenType("Bearer");

        return responseDTO;
    }
}





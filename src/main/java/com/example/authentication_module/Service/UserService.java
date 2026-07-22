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
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
@Service
public class UserService {
    public UserRepository repo;
    private final  PasswordEncoder passwordEncoder;
    public UserService(UserRepository repo , PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }
    public RegisterResponseDTO registerUser(RegisterRequestDTO requestDTO){
        if (repo.existsByEmail(requestDTO.getEmail()))
        {
            throw new EmailAlreadyexistsException("Email already exists!");
        }
        if(!requestDTO.getPassword().equals(requestDTO.getConfirmPassword()))
        {
            throw new PasswordMismatchException("Password Mismatch!");
        }
        if(!repo.existsByPhoneNumber((requestDTO.getPhoneNumber())))
        {
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

            UserModel savedUser= repo.save(user);
            return UserMapper.toResponse(savedUser);
        }


        public LoginResponseDTO loginUser(LoginRequestDTO requestDTO) {
            Optional<UserModel> optionalUser = repo.findByEmail(requestDTO.getEmail());

            if (optionalUser.isEmpty()) {
                throw new UserNotFoundException("User not found!");
            }

            UserModel user = optionalUser.get();
            if (!passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
                throw new PasswordMismatchException("Password Mismatch!");
            }
            if(!user.isEnabled()){
                throw new AccountLockedException("Account locked!");
            }
            if(!user.isAccountNonLocked()){
                throw new AccountLockedException("Account locked!");
            }
            if(!user.isCredentialsNonExpired()){
                throw new CredentialsExpiredException("Credentials expired!");
            }

            if(!user.isAccountNonExpired()){
                throw new AccountExpiredException("Account expired!");
            }
            user.setLoginTime(LocalDateTime.now());
            user.setLastAccessedAt(LocalDateTime.now());

            LoginResponseDTO responseDTO = new LoginResponseDTO(user.getEmail(), user.getUsername());
            responseDTO.setEmail(user.getEmail());
            responseDTO.setUsername(user.getUsername());
            responseDTO.setRole(user.getRole());

            String token = jwtService.generateToken(user);
            responseDTO.setToken(token);
            responseDTO.setTokenType("Bearer");
            responseDTO.setEmail(user.getEmail());
            responseDTO.setUsername(user.getUsername());
            responseDTO.setRole(user.getRole());
            return responseDTO;
        }

    }


package com.example.authentication_module.Controller;


import com.example.authentication_module.DTO.*;
import com.example.authentication_module.Service.RefreshTokenService;
import com.example.authentication_module.Service.UserService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshService;
    public UserController(UserService userService , RefreshTokenService refreshService) {
        this.userService = userService;
        this.refreshService = refreshService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> registerUser(
            @Valid @RequestBody RegisterRequestDTO registerRequestDTO) {

        RegisterResponseDTO response =
                userService.registerUser(registerRequestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        LoginResponseDTO response =
                userService.loginUser(loginRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @PutMapping("/change-password")
    public ResponseEntity<MessageResponseDTO> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequestDTO request) {

        MessageResponseDTO response =
                userService.changePassword(authentication.getName(), request);

        return ResponseEntity.ok(response);
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @RequestBody RefreshTokenRequestDTO request) {

        return ResponseEntity.ok(
                refreshService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestBody LogoutRequestDTO request) {

        userService.logout(request.getRefreshToken());

        return ResponseEntity.ok("Logged out successfully");
    }

}
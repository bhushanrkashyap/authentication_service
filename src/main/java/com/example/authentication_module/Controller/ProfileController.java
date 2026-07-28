package com.example.authentication_module.Controller;

import com.example.authentication_module.DTO.ProfileResponseDTO;
import com.example.authentication_module.DTO.UpdateProfileRequestDTO;
import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.Service.UserService;
import com.example.authentication_module.model.UserModel;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final UserService userService;

    public ProfileController(UserRepository userRepository,
                             UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/get-profile")
    public ProfileResponseDTO getProfile(Authentication authentication) {

        String email = authentication.getName();

        UserModel user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new ProfileResponseDTO(
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }

    @PutMapping("/update")
    public ProfileResponseDTO updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequestDTO request) {

        return userService.updateProfile(authentication.getName(), request);
    }

}
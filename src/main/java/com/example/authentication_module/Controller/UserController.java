package com.example.authentication_module.Controller;


import com.example.authentication_module.DTO.LoginRequestDTO;
import com.example.authentication_module.DTO.LoginResponseDTO;
import com.example.authentication_module.DTO.RegisterRequestDTO;
import com.example.authentication_module.DTO.RegisterResponseDTO;
import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.Service.UserService;
import com.example.authentication_module.model.UserModel;
import jakarta.validation.Valid;
import org.apache.catalina.User;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
}
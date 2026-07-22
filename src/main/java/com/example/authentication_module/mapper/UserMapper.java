package com.example.authentication_module.mapper;

import com.example.authentication_module.DTO.RegisterRequestDTO;
import com.example.authentication_module.DTO.RegisterResponseDTO;
import com.example.authentication_module.model.UserModel;

public class UserMapper {

        public static UserModel toEntity(RegisterRequestDTO dto) {

            UserModel user = new UserModel();

            user.setUsername(dto.getUserName());
            user.setEmail(dto.getEmail());
            user.setCity(dto.getCity());
            user.setState(dto.getState());
            user.setCountry(dto.getCountry());
            user.setPassword(dto.getPassword());
            user.setDateOfBirth(dto.getDateOfBirth());
            user.setAddress(dto.getAddress());
            user.setPhoneNumber(dto.getPhoneNumber());

            return user;
        }

        public static RegisterResponseDTO toResponse(UserModel user) {

            RegisterResponseDTO dto = new RegisterResponseDTO();

            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setRegisteredAt(user.getCreatedAt());

            return dto;
        }
    }

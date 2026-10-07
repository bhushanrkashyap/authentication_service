package com.example.authentication_module.Repository;

import com.example.authentication_module.model.UserModel;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<UserModel, Integer> {

    boolean existsByEmail(String email);
    Optional<UserModel> findByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<UserModel> findByPhoneNumber(String phoneNumber);
    Optional<UserModel> findByUsername(String username);
}

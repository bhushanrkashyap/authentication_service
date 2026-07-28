package com.example.authentication_module.Repository;

import com.example.authentication_module.model.UserModel;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<UserModel, Integer> {

    boolean existsByEmail(String email);
    Optional<UserModel> findByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
    
    List<UserModel> email(String email);
}

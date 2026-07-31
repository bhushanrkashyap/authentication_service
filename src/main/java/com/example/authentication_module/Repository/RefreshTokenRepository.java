package com.example.authentication_module.Repository;

import com.example.authentication_module.model.RefreshToken;
import com.example.authentication_module.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {


    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(UserModel user);

    void deleteByUser(UserModel user);

    void deleteByToken(String token);

    boolean existsByToken(String token);

    List<RefreshToken> findAllByUser(UserModel user);
}
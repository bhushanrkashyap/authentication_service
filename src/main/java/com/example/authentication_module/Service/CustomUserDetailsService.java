package com.example.authentication_module.Service;

import com.example.authentication_module.Repository.UserRepository;
import com.example.authentication_module.model.UserModel;
import com.example.authentication_module.model.UserPrincipal;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        System.out.println("email: " + email);
        UserModel user = repo.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
        System.out.println("looking for email" + email);
        return new UserPrincipal(user);
    }
}
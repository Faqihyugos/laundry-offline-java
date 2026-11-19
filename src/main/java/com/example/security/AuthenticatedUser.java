package com.example.security;

import com.example.domain.Role;
import com.example.domain.User;
import com.example.repository.UserRepository;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedUser {

    private final UserRepository userRepository;
    private final AuthenticationContext authenticationContext;

    public AuthenticatedUser(AuthenticationContext authenticationContext, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.authenticationContext = authenticationContext;
    }

    public Optional<User> get() {
        return authenticationContext.getAuthenticatedUser(UserDetails.class)
                .flatMap(userDetails -> userRepository.findByUsername(userDetails.getUsername()));
    }

    public boolean isOwner() {
        return get().map(u -> u.getRole() == Role.OWNER).orElse(false);
    }

    public boolean isCashier() {
        return get().map(u -> u.getRole() == Role.CASHIER).orElse(false);
    }

    public void logout() {
        authenticationContext.logout();
    }
}

package com.sanskaracademy.service;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findWithRoleByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
        
        // not verified / not approved -> login is refused with a friendly message (see SecurityConfig)
        boolean inactive = !user.isEmailVerified() || !user.isApprovedByAdmin();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getName())
                .disabled(inactive)
                .build();
    }
}
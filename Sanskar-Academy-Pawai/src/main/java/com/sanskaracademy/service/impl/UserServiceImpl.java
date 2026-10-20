package com.sanskaracademy.service.impl;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sanskaracademy.dto.RegisterDto;
import com.sanskaracademy.entity.Role;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.RoleRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.EmailService;
import com.sanskaracademy.service.TotpService;
import com.sanskaracademy.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final EmailService emailService;

    @Override
    public User createUser(User user, String roleName) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(role);
        user.setTotpSecret(totpService.generateSecret());

        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void regenerateSecretForUser(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTotpSecret(totpService.generateSecret());
        userRepository.save(user);
    }

    @Override
    public void generateSecretsForAllUsers() {

        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (user.getTotpSecret() == null || user.getTotpSecret().isEmpty()) {
                user.setTotpSecret(totpService.generateSecret());
                userRepository.save(user);
            }
        }
    }

    @Override
    public List<User> findAllStudents() {
        return userRepository.findAllStudents();
    }

	@Override
	public void registerUser(RegisterDto dto) {

	    if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
	        throw new RuntimeException("Username already exists");
	    }

	    Role role = roleRepository.findByName(dto.getRole())
	            .orElseThrow(() -> new RuntimeException("Invalid role"));

	    User user = new User();
	    user.setFullName(dto.getFullName());
	    user.setUsername(dto.getUsername());
	    user.setEmail(dto.getEmail());
	    user.setMobileNumber(dto.getMobileNumber());
	    user.setPassword(passwordEncoder.encode(dto.getPassword()));
	    user.setRole(role);
	    user.setEnabled(true);
	    user.setTotpSecret(totpService.generateSecret());
	    String token = UUID.randomUUID().toString();
	    user.setVerificationToken(token);
	    user.setEmailVerified(false);
	    user.setApprovedByAdmin(false);
	    user.setEnabled(false);

	    userRepository.save(user);

	    emailService.sendVerificationEmail(user.getEmail(), token);
	}

	@Override
	public @Nullable List<User> findAllTeachers() {
		return userRepository.findAllTeachers();
	}
}
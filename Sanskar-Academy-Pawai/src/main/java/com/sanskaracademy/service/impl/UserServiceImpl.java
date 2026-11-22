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
        user.setTotpSetupDone(false);

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
        user.setTotpSetupDone(false);      // user must scan the new QR at next login
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

	    if (dto.getUsername() == null || dto.getUsername().trim().length() < 4) {
	        throw new IllegalArgumentException("Username must be at least 4 characters");
	    }
	    if (dto.getPassword() == null || dto.getPassword().length() < 8) {
	        throw new IllegalArgumentException("Password must be at least 8 characters");
	    }
	    if (userRepository.findByUsername(dto.getUsername().trim()).isPresent()) {
	        throw new IllegalArgumentException("Username already exists");
	    }
	    if (dto.getEmail() != null && userRepository.findByEmail(dto.getEmail().trim()).isPresent()) {
	        throw new IllegalArgumentException("This email is already registered");
	    }
	    // self registration is only for these roles - admins are created by an admin
	    if (dto.getRole() == null || !List.of("STUDENT", "PARENT", "TEACHER").contains(dto.getRole())) {
	        throw new IllegalArgumentException("Please choose Student, Parent or Teacher");
	    }

	    Role role = roleRepository.findByName(dto.getRole())
	            .orElseThrow(() -> new IllegalArgumentException("Invalid role"));

	    User user = new User();
	    user.setFullName(dto.getFullName());
	    user.setUsername(dto.getUsername().trim());
	    user.setEmail(dto.getEmail() == null ? null : dto.getEmail().trim());
	    user.setMobileNumber(dto.getMobileNumber());
	    user.setPassword(passwordEncoder.encode(dto.getPassword()));
	    user.setRole(role);
	    user.setEnabled(true);
	    user.setTotpSecret(totpService.generateSecret());
	    user.setTotpSetupDone(false);
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
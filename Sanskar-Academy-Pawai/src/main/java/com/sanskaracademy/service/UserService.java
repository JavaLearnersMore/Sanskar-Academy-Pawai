package com.sanskaracademy.service;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.sanskaracademy.dto.RegisterDto;
import com.sanskaracademy.entity.User;



public interface UserService {

    User createUser(User user, String roleName);

    List<User> getAllUsers();

    void regenerateSecretForUser(String username);

    void generateSecretsForAllUsers();

    List<User> findAllStudents();
    
    void registerUser(RegisterDto registerDto);

	@Nullable
	List<User> findAllTeachers();
}
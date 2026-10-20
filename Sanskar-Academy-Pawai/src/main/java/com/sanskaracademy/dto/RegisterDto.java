package com.sanskaracademy.dto;

import lombok.Data;

@Data
public class RegisterDto {
	private String fullName;
	private String username;
	private String email;
	private String mobileNumber;
	private String password;
	private String role; 
}

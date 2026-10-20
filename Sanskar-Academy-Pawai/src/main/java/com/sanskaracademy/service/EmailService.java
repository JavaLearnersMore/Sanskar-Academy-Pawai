package com.sanskaracademy.service;

public interface EmailService {

	public void sendVerificationEmail(String to, String token);
}

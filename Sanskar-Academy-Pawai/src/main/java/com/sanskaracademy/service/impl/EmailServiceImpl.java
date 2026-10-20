package com.sanskaracademy.service.impl;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.sanskaracademy.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService{

	private final JavaMailSender mailSender;
	
	@Override
	public void sendVerificationEmail(String to, String token) {

        String link = "http://localhost:8080/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify Your Account - Sanskar Academy");
        message.setText("Click the link to verify your account: " + link);

        mailSender.send(message);
    }

}

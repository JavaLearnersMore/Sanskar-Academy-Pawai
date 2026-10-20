package com.sanskaracademy.service;

import org.springframework.stereotype.Service;

import com.sanskaracademy.entity.User;

@Service
public interface NotificationService {
	
	public void sendNotification(User recipient, String message);

}

package com.sanskaracademy.service.impl;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.sanskaracademy.entity.Notification;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.NotificationRepository;
import com.sanskaracademy.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{
	
	
	  private final NotificationRepository notificationRepository;

	@Override
	public void sendNotification(User recipient, String message) {

        Notification notification = Notification.builder()
                .recipient(recipient)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

}

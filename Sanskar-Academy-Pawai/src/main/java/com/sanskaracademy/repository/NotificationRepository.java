package com.sanskaracademy.repository;

import com.sanskaracademy.entity.Notification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.recipient.id = :userId
        AND n.isRead = false
    """)
    long countUnreadNotifications(@Param("userId") Long userId);
}
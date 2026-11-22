package com.sanskaracademy.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.*;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE a.loggedAt >= :from AND a.loggedAt < :to AND (:user = '' OR a.username = :user) AND (:method = '' OR a.method = :method) ORDER BY a.loggedAt DESC")
    List<AuditLog> search(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("user") String user,
                          @Param("method") String method, org.springframework.data.domain.Pageable page);
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM AuditLog a WHERE a.loggedAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}

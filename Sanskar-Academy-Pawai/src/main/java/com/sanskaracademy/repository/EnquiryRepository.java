package com.sanskaracademy.repository;

import java.time.*;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.*;

public interface EnquiryRepository extends JpaRepository<Enquiry, Long> {
    List<Enquiry> findAllByOrderByCreatedAtDesc();
    List<Enquiry> findByStatusOrderByCreatedAtDesc(EnquiryStatus status);
    @Query("SELECT e FROM Enquiry e WHERE e.nextFollowUp IS NOT NULL AND e.nextFollowUp <= :day AND e.status IN (:open) ORDER BY e.nextFollowUp")
    List<Enquiry> findDueFollowUps(@Param("day") LocalDate day, @Param("open") Collection<EnquiryStatus> open);
    long countByStatus(EnquiryStatus status);
    long countByCreatedAtAfter(LocalDateTime t);
}

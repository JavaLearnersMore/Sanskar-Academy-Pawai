package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.AttemptEvent;

public interface AttemptEventRepository extends JpaRepository<AttemptEvent, Long> {
    List<AttemptEvent> findByAttemptIdOrderByIdAsc(Long attemptId);
    long countByAttemptId(Long attemptId);
}

package com.sanskaracademy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.AttemptGuard;

public interface AttemptGuardRepository extends JpaRepository<AttemptGuard, Long> {
    Optional<AttemptGuard> findByAttemptId(Long attemptId);
}

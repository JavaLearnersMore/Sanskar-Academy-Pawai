package com.sanskaracademy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.ExamSecurity;

public interface ExamSecurityRepository extends JpaRepository<ExamSecurity, Long> {
    Optional<ExamSecurity> findByExamId(Long examId);
}

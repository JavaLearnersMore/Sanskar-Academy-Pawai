package com.sanskaracademy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.OfflineExam;

public interface OfflineExamRepository extends JpaRepository<OfflineExam, Long> {
    Optional<OfflineExam> findByExamId(Long examId);
    boolean existsByExamId(Long examId);
}

package com.sanskaracademy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.OfflineMark;

public interface OfflineMarkRepository extends JpaRepository<OfflineMark, Long> {
    List<OfflineMark> findByExamId(Long examId);
    Optional<OfflineMark> findByExamIdAndStudentId(Long examId, Long studentId);
}

package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.OfflineSheet;

public interface OfflineSheetRepository extends JpaRepository<OfflineSheet, Long> {
    List<OfflineSheet> findByMarkIdOrderByIdAsc(Long markId);
    List<OfflineSheet> findByMarkExamIdAndMarkStudentIdOrderByIdAsc(Long examId, Long studentId);
    List<OfflineSheet> findByMarkExamId(Long examId);
}

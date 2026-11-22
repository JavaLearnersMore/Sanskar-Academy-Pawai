package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.DiaryEntry;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    List<DiaryEntry> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<DiaryEntry> findTop100ByTeacherIdOrderByCreatedAtDesc(Long teacherId);
}

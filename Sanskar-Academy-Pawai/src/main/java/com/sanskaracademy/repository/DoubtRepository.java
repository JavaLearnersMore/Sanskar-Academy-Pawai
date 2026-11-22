package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.Doubt;

public interface DoubtRepository extends JpaRepository<Doubt, Long> {
    List<Doubt> findByStudentIdOrderByAskedAtDesc(Long studentId);
    List<Doubt> findBySubjectTeacherIdOrderByAskedAtDesc(Long teacherId);
    long countBySubjectTeacherIdAndAnswerIsNull(Long teacherId);
}

package com.sanskaracademy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.AttemptStatus;
import com.sanskaracademy.entity.ExamAttempt;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    List<ExamAttempt> findByExamId(Long examId);

    List<ExamAttempt> findByStudentIdAndStatus(Long studentId, AttemptStatus status);

    Optional<ExamAttempt> findByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamAttempt> findByStudentIdOrderByStartedAtDesc(Long studentId);

    List<ExamAttempt> findByExamIdAndStatusNotOrderBySubmittedAtDesc(Long examId, AttemptStatus status);

    List<ExamAttempt> findByExamSubjectTeacherIdAndStatusNot(Long teacherId, AttemptStatus status);

    long countByExamSubjectTeacherIdAndStatus(Long teacherId, AttemptStatus status);
}

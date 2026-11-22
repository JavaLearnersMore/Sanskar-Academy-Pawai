package com.sanskaracademy.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamStatus;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    List<Exam> findBySubjectTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Exam> findBySubjectClassEntityIdAndStatusInOrderByCreatedAtDesc(Long classId, Collection<ExamStatus> statuses);
}

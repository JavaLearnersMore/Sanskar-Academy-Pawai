package com.sanskaracademy.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.Homework;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    List<Homework> findBySubjectTeacherIdOrderByCreatedAtDesc(Long teacherId);
    List<Homework> findBySubjectClassEntityIdOrderByDueDateDescIdDesc(Long classId);
    List<Homework> findBySubjectIdInOrderByDueDateDescIdDesc(Collection<Long> subjectIds);
}

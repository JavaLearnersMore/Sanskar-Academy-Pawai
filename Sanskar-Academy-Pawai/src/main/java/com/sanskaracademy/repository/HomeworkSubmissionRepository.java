package com.sanskaracademy.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.HomeworkSubmission;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {
    Optional<HomeworkSubmission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);
    List<HomeworkSubmission> findByHomeworkId(Long homeworkId);
    List<HomeworkSubmission> findByStudentId(Long studentId);
    List<HomeworkSubmission> findByHomeworkIdIn(Collection<Long> homeworkIds);
    long countByHomeworkId(Long homeworkId);
}

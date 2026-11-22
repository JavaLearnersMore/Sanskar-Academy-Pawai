package com.sanskaracademy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.StudentEnrollment;

public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    Optional<StudentEnrollment> findByStudentId(Long studentId);

    List<StudentEnrollment> findByClassEntityId(Long classId);
}

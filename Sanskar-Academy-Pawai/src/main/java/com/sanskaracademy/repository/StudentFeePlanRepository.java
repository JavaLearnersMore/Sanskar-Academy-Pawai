package com.sanskaracademy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.StudentFeePlan;

public interface StudentFeePlanRepository extends JpaRepository<StudentFeePlan, Long> {

    Optional<StudentFeePlan> findByStudentId(Long studentId);
}

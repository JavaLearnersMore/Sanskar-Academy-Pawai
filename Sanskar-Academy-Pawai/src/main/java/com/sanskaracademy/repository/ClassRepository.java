package com.sanskaracademy.repository;

import com.sanskaracademy.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    Optional<ClassEntity> findByClassNameAndAcademicYear(String className, String academicYear);
}
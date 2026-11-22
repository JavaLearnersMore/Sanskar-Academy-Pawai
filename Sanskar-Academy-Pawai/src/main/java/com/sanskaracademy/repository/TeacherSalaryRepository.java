package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.TeacherSalary;

public interface TeacherSalaryRepository extends JpaRepository<TeacherSalary, Long> {

    List<TeacherSalary> findAllByOrderBySalaryYearDescSalaryMonthDescIdDesc();

    List<TeacherSalary> findByTeacherIdOrderBySalaryYearDescSalaryMonthDesc(Long teacherId);

    boolean existsByTeacherIdAndSalaryMonthAndSalaryYear(Long teacherId, int month, int year);
}

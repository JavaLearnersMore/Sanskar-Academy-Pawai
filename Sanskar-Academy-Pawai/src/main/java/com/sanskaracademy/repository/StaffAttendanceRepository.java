package com.sanskaracademy.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.StaffAttendance;

public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {
    List<StaffAttendance> findByAttDate(LocalDate date);
    Optional<StaffAttendance> findByStaffIdAndAttDate(Long staffId, LocalDate date);
    List<StaffAttendance> findByStaffIdAndAttDateBetweenOrderByAttDateAsc(Long staffId, LocalDate from, LocalDate to);
    List<StaffAttendance> findByAttDateBetween(LocalDate from, LocalDate to);
}

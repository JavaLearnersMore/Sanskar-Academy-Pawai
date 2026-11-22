package com.sanskaracademy.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.StaffPayroll;

public interface StaffPayrollRepository extends JpaRepository<StaffPayroll, Long> {
    Optional<StaffPayroll> findByStaffIdAndPayMonthAndPayYear(Long staffId, int month, int year);
    List<StaffPayroll> findByPayMonthAndPayYearOrderByStaffUserFullNameAsc(int month, int year);
    List<StaffPayroll> findByStaffIdOrderByPayYearDescPayMonthDesc(Long staffId);
}

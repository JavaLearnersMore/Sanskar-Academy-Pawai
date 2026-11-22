package com.sanskaracademy.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sanskaracademy.entity.Holiday;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByHolidayDate(LocalDate date);
    boolean existsByHolidayDate(LocalDate date);
    List<Holiday> findByHolidayDateBetween(LocalDate from, LocalDate to);
    List<Holiday> findByHolidayDateGreaterThanEqualOrderByHolidayDateAsc(LocalDate from);
    List<Holiday> findAllByOrderByHolidayDateDesc();
}

package com.sanskaracademy.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.StaffLeave;
import com.sanskaracademy.entity.StaffLeaveStatus;

public interface StaffLeaveRepository extends JpaRepository<StaffLeave, Long> {
    List<StaffLeave> findAllByOrderByAppliedAtDesc();
    List<StaffLeave> findByStaffIdOrderByFromDateDesc(Long staffId);
    long countByStatus(StaffLeaveStatus status);

    @Query("SELECT l FROM StaffLeave l WHERE l.staff.id = :sid AND l.status IN :st AND l.fromDate <= :to AND l.toDate >= :from")
    List<StaffLeave> findOverlapping(@Param("sid") Long staffId, @Param("st") Collection<StaffLeaveStatus> statuses,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);
}

package com.sanskaracademy.repository;

import com.sanskaracademy.entity.Attendance;
import com.sanskaracademy.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentId(Long studentId);

    List<Attendance> findByStudentIdAndAttendanceDate(
            Long studentId, LocalDate date);

    @Query("""
        SELECT COUNT(a) FROM Attendance a
        WHERE a.student.id = :studentId
        AND a.status = :status
    """)
    long countAttendanceByStatus(@Param("studentId") Long studentId,
                                  @Param("status") AttendanceStatus status);

    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.subject
        WHERE a.student.id = :studentId
    """)
    List<Attendance> findWithSubjectByStudent(@Param("studentId") Long studentId);
}
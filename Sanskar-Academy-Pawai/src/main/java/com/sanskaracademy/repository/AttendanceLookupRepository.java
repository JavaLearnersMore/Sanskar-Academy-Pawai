package com.sanskaracademy.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.Attendance;

/** read-only helper: on which dates did a teacher mark attendance for a subject (existing AttendanceRepository is not changed) */
public interface AttendanceLookupRepository extends JpaRepository<Attendance, Long> {

    /** rows of [subjectId, attendanceDate] */
    @Query("SELECT DISTINCT a.subject.id, a.attendanceDate FROM Attendance a WHERE a.attendanceDate BETWEEN :from AND :to")
    List<Object[]> markedSubjectDates(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

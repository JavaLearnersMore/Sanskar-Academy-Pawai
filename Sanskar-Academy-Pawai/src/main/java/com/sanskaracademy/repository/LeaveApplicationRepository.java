package com.sanskaracademy.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.LeaveApplication;
import com.sanskaracademy.entity.LeaveStatus;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {

    List<LeaveApplication> findAllByOrderByAppliedAtDesc();

    List<LeaveApplication> findByApplicantIdOrderByAppliedAtDesc(Long applicantId);

    List<LeaveApplication> findByStudentIdOrderByAppliedAtDesc(Long studentId);

    long countByStatus(LeaveStatus status);

    @Query("""
        SELECT COUNT(l) FROM LeaveApplication l
        WHERE l.student.id = :studentId
          AND l.status <> :rejected
          AND l.fromDate <= :toDate AND l.toDate >= :fromDate
    """)
    long countStudentOverlap(@Param("studentId") Long studentId,
                             @Param("rejected") LeaveStatus rejected,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT COUNT(l) FROM LeaveApplication l
        WHERE l.applicant.id = :teacherId
          AND l.student IS NULL
          AND l.status <> :rejected
          AND l.fromDate <= :toDate AND l.toDate >= :fromDate
    """)
    long countTeacherOverlap(@Param("teacherId") Long teacherId,
                             @Param("rejected") LeaveStatus rejected,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate);

    /** Leaves of students who are in a class this teacher teaches. */
    @Query("""
        SELECT l FROM LeaveApplication l
        WHERE l.student.id IN (
            SELECT e.student.id FROM StudentEnrollment e
            WHERE e.classEntity.id IN (
                SELECT s.classEntity.id FROM Subject s WHERE s.teacher.id = :teacherId))
        ORDER BY l.appliedAt DESC
    """)
    List<LeaveApplication> findStudentLeavesForTeacher(@Param("teacherId") Long teacherId);

    /** Students having a leave with this status on this date. */
    @Query("""
        SELECT l.student.id FROM LeaveApplication l
        WHERE l.status = :status
          AND l.student IS NOT NULL
          AND l.fromDate <= :date AND l.toDate >= :date
    """)
    List<Long> findStudentIdsOnLeave(@Param("status") LeaveStatus status,
                                     @Param("date") LocalDate date);
}

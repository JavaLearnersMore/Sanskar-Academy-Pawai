package com.sanskaracademy.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.LectureSession;

public interface LectureSessionRepository extends JpaRepository<LectureSession, Long> {

    @Query("""
        SELECT s FROM LectureSession s
        JOIN FETCH s.subject sub
        JOIN FETCH sub.classEntity
        LEFT JOIN FETCH sub.teacher
        LEFT JOIN FETCH s.substituteTeacher
        WHERE s.lectureDate BETWEEN :from AND :to
        ORDER BY s.lectureDate, s.startTime, s.id
    """)
    List<LectureSession> findRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    boolean existsBySubjectIdAndLectureDateAndStartTime(Long subjectId, LocalDate date, LocalTime start);

    /** overlapping, not cancelled lectures of the same class or of the same teacher on that date */
    @Query("""
        SELECT COUNT(s) FROM LectureSession s
        WHERE s.id <> :excludeId
          AND s.lectureDate = :date
          AND s.status <> com.sanskaracademy.entity.LectureStatus.CANCELLED
          AND s.startTime < :endTime
          AND s.endTime > :startTime
          AND (s.subject.classEntity.id = :classId
               OR s.substituteTeacher.id = :teacherId
               OR (s.substituteTeacher IS NULL AND s.subject.teacher.id = :teacherId))
    """)
    long countClashes(@Param("excludeId") Long excludeId,
                      @Param("date") LocalDate date,
                      @Param("startTime") LocalTime startTime,
                      @Param("endTime") LocalTime endTime,
                      @Param("classId") Long classId,
                      @Param("teacherId") Long teacherId);
}

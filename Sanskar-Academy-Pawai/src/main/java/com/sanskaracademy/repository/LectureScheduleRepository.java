package com.sanskaracademy.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.LectureSchedule;

public interface LectureScheduleRepository extends JpaRepository<LectureSchedule, Long> {

    @Query("SELECT l FROM LectureSchedule l WHERE l.subject.teacher.id = :teacherId")
    List<LectureSchedule> findByTeacher(@Param("teacherId") Long teacherId);

    @Query("SELECT l FROM LectureSchedule l WHERE l.subject.classEntity.id = :classId")
    List<LectureSchedule> findByClassId(@Param("classId") Long classId);

    /** Existing lectures overlapping this slot for the same teacher or the same class. */
    @Query("""
        SELECT COUNT(l) FROM LectureSchedule l
        WHERE l.dayOfWeek = :day
          AND l.startTime < :endTime
          AND l.endTime > :startTime
          AND (l.subject.teacher.id = :teacherId OR l.subject.classEntity.id = :classId)
    """)
    long countClashes(@Param("day") DayOfWeek day,
                      @Param("startTime") LocalTime startTime,
                      @Param("endTime") LocalTime endTime,
                      @Param("teacherId") Long teacherId,
                      @Param("classId") Long classId);

    /** Same check while editing: the lecture being edited is not counted against itself. */
    @Query("""
        SELECT COUNT(l) FROM LectureSchedule l
        WHERE l.id <> :excludeId
          AND l.dayOfWeek = :day
          AND l.startTime < :endTime
          AND l.endTime > :startTime
          AND (l.subject.teacher.id = :teacherId OR l.subject.classEntity.id = :classId)
    """)
    long countClashesExcluding(@Param("excludeId") Long excludeId,
                               @Param("day") DayOfWeek day,
                               @Param("startTime") LocalTime startTime,
                               @Param("endTime") LocalTime endTime,
                               @Param("teacherId") Long teacherId,
                               @Param("classId") Long classId);
}

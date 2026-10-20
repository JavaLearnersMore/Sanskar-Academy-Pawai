package com.sanskaracademy.repository;

import com.sanskaracademy.entity.Subject;
import com.sanskaracademy.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByTeacher(User teacher);

    @Query("""
        SELECT s FROM Subject s
        JOIN FETCH s.classEntity
        WHERE s.teacher.id = :teacherId
    """)
    List<Subject> findSubjectsByTeacherId(@Param("teacherId") Long teacherId);

    List<Subject> findByClassEntityId(Long classId);
}
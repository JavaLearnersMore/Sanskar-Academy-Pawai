package com.sanskaracademy.repository;

import com.sanskaracademy.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentParentRepository extends JpaRepository<User, Long> {

    @Query(value = """
        SELECT u.* FROM users u
        JOIN student_parent sp ON u.id = sp.parent_id
        WHERE sp.student_id = :studentId
    """, nativeQuery = true)
    List<User> findParentsByStudent(@Param("studentId") Long studentId);

    @Query(value = """
        SELECT u.* FROM users u
        JOIN student_parent sp ON u.id = sp.student_id
        WHERE sp.parent_id = :parentId
    """, nativeQuery = true)
    List<User> findStudentsByParent(@Param("parentId") Long parentId);
}
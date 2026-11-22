package com.sanskaracademy.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.User;

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

    // NEW - used by the admin "Parent links" tab
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO student_parent (student_id, parent_id) VALUES (:studentId, :parentId)",
           nativeQuery = true)
    void link(@Param("studentId") Long studentId, @Param("parentId") Long parentId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM student_parent WHERE student_id = :studentId AND parent_id = :parentId",
           nativeQuery = true)
    void unlink(@Param("studentId") Long studentId, @Param("parentId") Long parentId);
}

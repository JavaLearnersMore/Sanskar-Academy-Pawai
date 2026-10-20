package com.sanskaracademy.repository;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.entity.Role;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findByRole(Role role);

    @Query("""
        SELECT u FROM User u
        JOIN FETCH u.role
        WHERE u.username = :username
    """)
    Optional<User> findWithRoleByUsername(@Param("username") String username);
    
    @Query("""
    	    SELECT u FROM User u
    	    WHERE u.role.name = 'STUDENT'
    	""")
    	List<User> findAllStudents();

	long countByRoleName(String string);

	@Nullable
	Object findByEmailVerifiedTrueAndApprovedByAdminFalse();

	Optional<User> findByVerificationToken(String token);
	
	@Query("SELECT u FROM User u WHERE u.role.name = 'TEACHER'")
	List<User> findAllTeachers();

	@Query("SELECT u FROM User u WHERE u.approvedByAdmin = false")
	List<User> findPendingUsers();
}
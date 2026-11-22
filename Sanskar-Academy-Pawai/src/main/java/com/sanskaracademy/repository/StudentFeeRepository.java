package com.sanskaracademy.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sanskaracademy.entity.StudentFee;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {

    @Query("SELECT COALESCE(SUM(f.amountPaid),0) FROM StudentFee f")
    BigDecimal getTotalIncome();

    @Query("SELECT COALESCE(SUM(f.amountPaid),0) FROM StudentFee f WHERE f.student.id = :studentId")
    BigDecimal sumPaidByStudentId(@Param("studentId") Long studentId);

    /** rows of [studentId, totalPaid] */
    @Query("SELECT f.student.id, SUM(f.amountPaid) FROM StudentFee f GROUP BY f.student.id")
    List<Object[]> sumPaidGroupedByStudent();

    List<StudentFee> findByStudentIdOrderByPaymentDateDescIdDesc(Long studentId);
}

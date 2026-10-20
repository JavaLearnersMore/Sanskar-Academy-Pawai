package com.sanskaracademy.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sanskaracademy.entity.StudentFee;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {

    @Query("SELECT COALESCE(SUM(f.amountPaid),0) FROM StudentFee f")
    BigDecimal getTotalIncome();
}

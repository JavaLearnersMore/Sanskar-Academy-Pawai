package com.sanskaracademy.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sanskaracademy.entity.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT COALESCE(SUM(e.amount),0) FROM Expense e")
    BigDecimal getTotalExpenses();

	List<Expense> findAllByOrderByExpenseDateDesc();
}
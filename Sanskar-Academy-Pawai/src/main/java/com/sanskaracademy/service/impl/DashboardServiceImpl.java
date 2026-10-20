package com.sanskaracademy.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.ExpenseRepository;
import com.sanskaracademy.repository.StudentFeeRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.DashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final ExpenseRepository expenseRepository;

    @Override
    public Map<String, Object> getAdminDashboardStats() {

        Map<String, Object> stats = new HashMap<>();

        List<User> students = userRepository.findAllStudents();
        long teachers = userRepository.countByRoleName("TEACHER");

//        Double income = studentFeeRepository.getTotalIncome();
//        Double expenses = expenseRepository.getTotalExpenses();
        
        BigDecimal income = studentFeeRepository.getTotalIncome();
        BigDecimal expenses = expenseRepository.getTotalExpenses();

        if (income == null) income = BigDecimal.ZERO;
        if (expenses == null) expenses = BigDecimal.ZERO;

        BigDecimal profit = income.subtract(expenses);

        stats.put("totalStudents", students.size());
        stats.put("totalTeachers", teachers);
        stats.put("totalIncome", income);
        stats.put("netProfit", profit);

        return stats;
    }
}
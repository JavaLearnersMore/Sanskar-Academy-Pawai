package com.sanskaracademy.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.entity.Expense;
import com.sanskaracademy.entity.ExpenseCategory;
import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.TeacherSalary;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.ExpenseRepository;
import com.sanskaracademy.repository.TeacherSalaryRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.NotificationService;
import com.sanskaracademy.service.SalaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private final TeacherSalaryRepository salaryRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSalary> getAll() {
        return salaryRepository.findAllByOrderBySalaryYearDescSalaryMonthDescIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherSalary> getForTeacher(Long teacherId) {
        return salaryRepository.findByTeacherIdOrderBySalaryYearDescSalaryMonthDesc(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherSalary get(Long id) {
        return salaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Salary record not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getLastBaseAmounts() {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (TeacherSalary s : getAll()) {                 // newest first
            map.putIfAbsent(s.getTeacher().getId(), s.getBaseAmount());
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getPaidTeacherIds(int month, int year) {
        Set<Long> ids = new HashSet<>();
        for (TeacherSalary s : getAll()) {
            if (s.getSalaryMonth() == month && s.getSalaryYear() == year) {
                ids.add(s.getTeacher().getId());
            }
        }
        return ids;
    }

    @Override
    @Transactional
    public TeacherSalary paySalary(Long teacherId, int month, int year,
                                   BigDecimal base, BigDecimal bonus, BigDecimal deduction,
                                   PaymentMode mode, LocalDate paymentDate, String remarks, User paidBy) {

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        if (teacher.getRole() == null || !"TEACHER".equals(teacher.getRole().getName())) {
            throw new IllegalArgumentException("Selected user is not a teacher");
        }
        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Invalid salary month or year");
        }
        if (base == null || base.signum() <= 0) {
            throw new IllegalArgumentException("Base salary must be greater than 0");
        }
        if (bonus == null) bonus = BigDecimal.ZERO;
        if (deduction == null) deduction = BigDecimal.ZERO;
        if (bonus.signum() < 0 || deduction.signum() < 0) {
            throw new IllegalArgumentException("Bonus and deduction cannot be negative");
        }
        BigDecimal net = base.add(bonus).subtract(deduction);
        if (net.signum() <= 0) {
            throw new IllegalArgumentException("Net salary must be greater than 0");
        }
        if (salaryRepository.existsByTeacherIdAndSalaryMonthAndSalaryYear(teacherId, month, year)) {
            throw new IllegalArgumentException("Salary for this month is already paid to this teacher");
        }

        LocalDate date = paymentDate != null ? paymentDate : LocalDate.now();

        TeacherSalary s = new TeacherSalary();
        s.setTeacher(teacher);
        s.setSalaryMonth(month);
        s.setSalaryYear(year);
        s.setBaseAmount(base);
        s.setBonus(bonus);
        s.setDeduction(deduction);
        s.setNetAmount(net);
        s.setPaymentDate(date);
        s.setPaymentMode(mode != null ? mode : PaymentMode.BANK_TRANSFER);
        s.setRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        s.setPaidBy(paidBy);
        s = salaryRepository.save(s);

        s.setSlipNumber("SLP-" + String.format("%04d%02d", year, month) + "-" + String.format("%04d", s.getId()));
        s = salaryRepository.save(s);

        // record as an expense so Finance / Net Profit include it automatically
        Expense expense = new Expense();
        expense.setExpenseTitle("Salary - " + teacher.getFullName() + " (" + s.getPeriodLabel() + ")");
        expense.setDescription("Salary slip " + s.getSlipNumber());
        expense.setAmount(net);
        expense.setExpenseDate(date);
        expense.setCategory(ExpenseCategory.SALARY);
        expense.setRecordedBy(paidBy);
        expenseRepository.save(expense);

        notificationService.sendNotification(teacher,
                "Your salary of ₹" + net + " for " + s.getPeriodLabel()
                        + " has been paid (" + s.getSlipNumber() + ").");
        return s;
    }
}

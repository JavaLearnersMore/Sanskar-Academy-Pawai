package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.TeacherSalary;
import com.sanskaracademy.entity.User;

public interface SalaryService {

    List<TeacherSalary> getAll();

    List<TeacherSalary> getForTeacher(Long teacherId);

    TeacherSalary get(Long id);

    /** teacherId -> base amount of the last salary paid (pre-fills the form). */
    Map<Long, BigDecimal> getLastBaseAmounts();

    /** teacher ids already paid for the given month. */
    Set<Long> getPaidTeacherIds(int month, int year);

    TeacherSalary paySalary(Long teacherId, int month, int year,
                            BigDecimal base, BigDecimal bonus, BigDecimal deduction,
                            PaymentMode mode, LocalDate paymentDate, String remarks, User paidBy);
}

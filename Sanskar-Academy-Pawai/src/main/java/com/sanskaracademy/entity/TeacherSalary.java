package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** One salary payment to a teacher for one month. */
@Entity
@Table(name = "teacher_salary",
       uniqueConstraints = @UniqueConstraint(columnNames = {"teacher_id", "salary_month", "salary_year"}))
@Getter
@Setter
public class TeacherSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(name = "salary_month", nullable = false)
    private int salaryMonth;

    @Column(name = "salary_year", nullable = false)
    private int salaryYear;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal baseAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal deduction = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal netAmount;

    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String remarks;

    private String slipNumber;

    @ManyToOne
    @JoinColumn(name = "paid_by")
    private User paidBy;

    /** e.g. "November 2026" (not stored) */
    public String getPeriodLabel() {
        return Month.of(salaryMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + salaryYear;
    }
}

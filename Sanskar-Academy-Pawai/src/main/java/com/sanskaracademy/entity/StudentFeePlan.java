package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * How much fee a student has to pay (one plan per student).
 * Payments are stored in StudentFee.
 */
@Entity
@Table(name = "student_fee_plan",
       uniqueConstraints = @UniqueConstraint(columnNames = "student_id"))
@Getter
@Setter
public class StudentFeePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal totalFee;

    @Column(precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    private String academicYear;

    private LocalDate dueDate;
}

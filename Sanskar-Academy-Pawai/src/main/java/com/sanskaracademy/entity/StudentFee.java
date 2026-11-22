package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/** One fee payment (installment) made by a student. */
@Entity
@Getter @Setter
public class StudentFee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User student;

    // kept for backward compatibility - not required by the new fee module
    @ManyToOne
    private FeeStructure feeStructure;

    private BigDecimal amountPaid;
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String receiptNumber;

    // NEW
    private String remarks;
}

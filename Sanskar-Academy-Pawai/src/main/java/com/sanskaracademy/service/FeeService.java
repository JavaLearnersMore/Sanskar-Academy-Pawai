package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.sanskaracademy.dto.FeeSummaryDto;
import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.StudentFee;

public interface FeeService {

    /** One row per student (including students with no fee set yet). */
    List<FeeSummaryDto> getAllFeeSummaries();

    FeeSummaryDto getSummary(Long studentId);

    /** Create or update the fee plan of a student. */
    void setFeePlan(Long studentId, BigDecimal totalFee, BigDecimal discount,
                    String academicYear, LocalDate dueDate);

    /** Record a payment and generate a receipt number. */
    StudentFee collectPayment(Long studentId, BigDecimal amount, PaymentMode mode,
                              LocalDate paymentDate, String remarks);

    List<StudentFee> getPayments(Long studentId);

    StudentFee getPayment(Long paymentId);
}

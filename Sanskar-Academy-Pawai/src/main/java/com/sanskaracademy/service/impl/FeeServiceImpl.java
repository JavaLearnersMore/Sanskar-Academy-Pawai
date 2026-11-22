package com.sanskaracademy.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.dto.FeeSummaryDto;
import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.StudentFee;
import com.sanskaracademy.entity.StudentFeePlan;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.StudentFeePlanRepository;
import com.sanskaracademy.repository.StudentFeeRepository;
import com.sanskaracademy.repository.StudentParentRepository;
import com.sanskaracademy.repository.UserRepository;
import com.sanskaracademy.service.FeeService;
import com.sanskaracademy.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    private final UserRepository userRepository;
    private final StudentFeePlanRepository planRepository;
    private final StudentFeeRepository paymentRepository;
    private final NotificationService notificationService;
    private final StudentParentRepository studentParentRepository;

    // ---------------------------------------------------------------
    // READ
    // ---------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<FeeSummaryDto> getAllFeeSummaries() {

        Map<Long, StudentFeePlan> plans = new HashMap<>();
        for (StudentFeePlan p : planRepository.findAll()) {
            plans.put(p.getStudent().getId(), p);
        }

        Map<Long, BigDecimal> paidMap = new HashMap<>();
        for (Object[] row : paymentRepository.sumPaidGroupedByStudent()) {
            paidMap.put((Long) row[0], (BigDecimal) row[1]);
        }

        List<FeeSummaryDto> result = new ArrayList<>();
        for (User student : userRepository.findAllStudents()) {
            BigDecimal paid = paidMap.getOrDefault(student.getId(), BigDecimal.ZERO);
            result.add(build(student, plans.get(student.getId()), paid));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public FeeSummaryDto getSummary(Long studentId) {
        User student = findStudent(studentId);
        StudentFeePlan plan = planRepository.findByStudentId(studentId).orElse(null);
        BigDecimal paid = paymentRepository.sumPaidByStudentId(studentId);
        return build(student, plan, paid == null ? BigDecimal.ZERO : paid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentFee> getPayments(Long studentId) {
        return paymentRepository.findByStudentIdOrderByPaymentDateDescIdDesc(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFee getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
    }

    // ---------------------------------------------------------------
    // WRITE
    // ---------------------------------------------------------------
    @Override
    @Transactional
    public void setFeePlan(Long studentId, BigDecimal totalFee, BigDecimal discount,
                           String academicYear, LocalDate dueDate) {

        User student = findStudent(studentId);

        if (totalFee == null || totalFee.signum() <= 0) {
            throw new IllegalArgumentException("Total fee must be greater than 0");
        }
        if (discount == null) discount = BigDecimal.ZERO;
        if (discount.signum() < 0 || discount.compareTo(totalFee) > 0) {
            throw new IllegalArgumentException("Discount must be between 0 and the total fee");
        }

        BigDecimal paid = paymentRepository.sumPaidByStudentId(studentId);
        if (paid == null) paid = BigDecimal.ZERO;
        BigDecimal payable = totalFee.subtract(discount);
        if (payable.compareTo(paid) < 0) {
            throw new IllegalArgumentException(
                    "Payable amount cannot be less than the already paid amount (" + paid + ")");
        }

        StudentFeePlan plan = planRepository.findByStudentId(studentId)
                .orElseGet(StudentFeePlan::new);
        plan.setStudent(student);
        plan.setTotalFee(totalFee);
        plan.setDiscount(discount);
        plan.setAcademicYear(academicYear);
        plan.setDueDate(dueDate);
        planRepository.save(plan);
    }

    @Override
    @Transactional
    public StudentFee collectPayment(Long studentId, BigDecimal amount, PaymentMode mode,
                                     LocalDate paymentDate, String remarks) {

        User student = findStudent(studentId);

        FeeSummaryDto summary = getSummary(studentId);
        if ("NOT_SET".equals(summary.getStatus())) {
            throw new IllegalArgumentException("Set the fee for this student before collecting payment");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (amount.compareTo(summary.getPending()) > 0) {
            throw new IllegalArgumentException(
                    "Amount is more than the pending fee (" + summary.getPending() + ")");
        }

        LocalDate date = paymentDate != null ? paymentDate : LocalDate.now();

        StudentFee fee = new StudentFee();
        fee.setStudent(student);
        fee.setAmountPaid(amount);
        fee.setPaymentDate(date);
        fee.setPaymentMode(mode != null ? mode : PaymentMode.CASH);
        fee.setRemarks(remarks == null || remarks.isBlank() ? null : remarks.trim());
        fee = paymentRepository.save(fee);

        // receipt number needs the generated id
        fee.setReceiptNumber("SAC-" + date.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%05d", fee.getId()));
        fee = paymentRepository.save(fee);

        // tell the student and the parents
        notificationService.sendNotification(student,
                "Fee payment of ₹" + amount + " received. Receipt " + fee.getReceiptNumber() + ".");
        for (User parent : studentParentRepository.findParentsByStudent(studentId)) {
            notificationService.sendNotification(parent,
                    "Fee payment of ₹" + amount + " received for " + student.getFullName()
                            + ". Receipt " + fee.getReceiptNumber() + ".");
        }
        return fee;
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------
    private User findStudent(Long studentId) {
        User u = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (u.getRole() == null || !"STUDENT".equals(u.getRole().getName())) {
            throw new IllegalArgumentException("Selected user is not a student");
        }
        return u;
    }

    private FeeSummaryDto build(User student, StudentFeePlan plan, BigDecimal paid) {

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        if (plan != null) {
            if (plan.getTotalFee() != null) total = plan.getTotalFee();
            if (plan.getDiscount() != null) discount = plan.getDiscount();
        }

        BigDecimal payable = total.subtract(discount).max(BigDecimal.ZERO);
        BigDecimal pending = payable.subtract(paid).max(BigDecimal.ZERO);

        String status;
        if (plan == null) {
            status = "NOT_SET";
        } else if (pending.signum() == 0) {
            status = "PAID";
        } else if (paid.signum() == 0) {
            status = "UNPAID";
        } else {
            status = "PARTIAL";
        }

        int percent = 0;
        if (payable.signum() > 0) {
            percent = paid.multiply(BigDecimal.valueOf(100))
                    .divide(payable, 0, RoundingMode.DOWN)
                    .min(BigDecimal.valueOf(100)).intValue();
        }

        boolean overdue = plan != null && pending.signum() > 0
                && plan.getDueDate() != null && plan.getDueDate().isBefore(LocalDate.now());

        return FeeSummaryDto.builder()
                .studentId(student.getId())
                .studentName(student.getFullName())
                .email(student.getEmail())
                .mobile(student.getMobileNumber())
                .academicYear(plan != null ? plan.getAcademicYear() : null)
                .dueDate(plan != null ? plan.getDueDate() : null)
                .totalFee(total)
                .discount(discount)
                .payable(payable)
                .paid(paid)
                .pending(pending)
                .percentPaid(percent)
                .overdue(overdue)
                .status(status)
                .build();
    }
}

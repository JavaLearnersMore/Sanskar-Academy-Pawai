package com.sanskaracademy.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sanskaracademy.dto.FeeSummaryDto;
import com.sanskaracademy.entity.FeePaymentRequest;
import com.sanskaracademy.entity.PaymentMode;
import com.sanskaracademy.entity.RequestStatus;
import com.sanskaracademy.entity.StudentFee;
import com.sanskaracademy.entity.User;
import com.sanskaracademy.repository.FeePaymentRequestRepository;
import com.sanskaracademy.repository.UserRepository;

/**
 * Parent pays the school by UPI / bank transfer outside the app, then sends the payment reference here.
 * The admin checks the money in the bank and approves; only then the fee is recorded and a receipt is created.
 */
@Service
public class FeeRequestService {

    private final FeePaymentRequestRepository repo;
    private final UserRepository users;
    private final FeeService fees;
    private final SchoolAccess access;
    private final String upiId;
    private final String payeeName;

    public FeeRequestService(FeePaymentRequestRepository repo, UserRepository users, FeeService fees, SchoolAccess access,
                             @Value("${app.fees.upi-id:}") String upiId,
                             @Value("${app.fees.payee-name:Sanskar Academy Classes}") String payeeName) {
        this.repo = repo;
        this.users = users;
        this.fees = fees;
        this.access = access;
        this.upiId = upiId == null ? "" : upiId.trim();
        this.payeeName = payeeName;
    }

    public String getUpiId() { return upiId; }
    public String getPayeeName() { return payeeName; }

    @Transactional
    public FeePaymentRequest submit(User parent, Long studentId, BigDecimal amount, PaymentMode mode, String reference,
                                    LocalDate paidOn, String note) {
        if (!access.isParentOf(parent.getId(), studentId)) throw new IllegalArgumentException("This student is not linked to your account");
        if (mode == null || mode == PaymentMode.CASH) throw new IllegalArgumentException("Choose UPI, card or bank transfer. For cash, pay at the office.");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Enter the amount you paid");
        String ref = reference == null ? "" : reference.trim();
        if (ref.length() < 6) throw new IllegalArgumentException("Enter the UPI reference / UTR number (at least 6 characters)");
        if (ref.length() > 80) ref = ref.substring(0, 80);
        if (paidOn == null) paidOn = LocalDate.now();
        if (paidOn.isAfter(LocalDate.now())) throw new IllegalArgumentException("The payment date cannot be in the future");
        FeeSummaryDto sum = fees.getSummary(studentId);
        if (sum == null || sum.getPending() == null || sum.getPending().signum() <= 0)
            throw new IllegalArgumentException("There is no pending fee for this student");
        if (amount.compareTo(sum.getPending()) > 0)
            throw new IllegalArgumentException("The amount is more than the pending fee (Rs. " + sum.getPending() + ")");
        if (repo.existsByReferenceIgnoreCaseAndStatusIn(ref, EnumSet.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
            throw new IllegalArgumentException("This reference number was already submitted");
        User kid = users.findById(studentId).orElseThrow(() -> new IllegalArgumentException("Student not found"));
        FeePaymentRequest r = new FeePaymentRequest();
        r.setStudent(kid);
        r.setParent(parent);
        r.setAmount(amount);
        r.setMode(mode);
        r.setReference(ref);
        r.setPaidOn(paidOn);
        r.setNote(note == null || note.isBlank() ? null : (note.trim().length() > 250 ? note.trim().substring(0, 250) : note.trim()));
        r.setStatus(RequestStatus.PENDING);
        r.setCreatedAt(LocalDateTime.now());
        repo.save(r);
        for (User a : users.findAll()) if (SchoolAccess.role(a).equals("ADMIN"))
            access.tell(a, parent.getFullName() + " sent a fee payment of Rs. " + amount + " for " + kid.getFullName() + " (ref " + ref + "). Please verify and approve.");
        return r;
    }

    @Transactional(readOnly = true)
    public List<FeePaymentRequest> ofParent(Long parentId) {
        return repo.findByParentIdOrderByCreatedAtDesc(parentId);
    }

    @Transactional(readOnly = true)
    public List<FeePaymentRequest> pending() {
        return repo.findByStatusOrderByCreatedAtAsc(RequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<FeePaymentRequest> recent() {
        return repo.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return repo.countByStatus(RequestStatus.PENDING);
    }

    @Transactional
    public void approve(Long id, User admin, String adminNote) {
        FeePaymentRequest r = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (r.getStatus() != RequestStatus.PENDING) throw new IllegalArgumentException("This request is already " + r.getStatus().name().toLowerCase());
        StudentFee f = fees.collectPayment(r.getStudent().getId(), r.getAmount(), r.getMode(), r.getPaidOn(),
                "Ref " + r.getReference() + " - sent by parent " + r.getParent().getFullName() + ", approved by " + admin.getFullName());
        r.setStatus(RequestStatus.APPROVED);
        r.setDecidedBy(admin);
        r.setDecidedAt(LocalDateTime.now());
        r.setAdminNote(clip(adminNote));
        r.setPaymentId(f.getId());
        repo.save(r);   // @Version stops a double approval
        access.tell(r.getParent(), "Your payment of Rs. " + r.getAmount() + " for " + r.getStudent().getFullName() + " was received. Receipt is ready under Pay Fees.");
    }

    @Transactional
    public void reject(Long id, User admin, String adminNote) {
        FeePaymentRequest r = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (r.getStatus() != RequestStatus.PENDING) throw new IllegalArgumentException("This request is already " + r.getStatus().name().toLowerCase());
        if (adminNote == null || adminNote.isBlank()) throw new IllegalArgumentException("Please write the reason for rejecting");
        r.setStatus(RequestStatus.REJECTED);
        r.setDecidedBy(admin);
        r.setDecidedAt(LocalDateTime.now());
        r.setAdminNote(clip(adminNote));
        repo.save(r);
        access.tell(r.getParent(), "Your fee payment reference " + r.getReference() + " could not be verified: " + r.getAdminNote());
    }

    private static String clip(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        return s.length() > 250 ? s.substring(0, 250) : s;
    }
}

package com.sanskaracademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
import lombok.Getter;
import lombok.Setter;

/**
 * Leave request.
 *  - student leave : student = the student, applicant = student OR parent
 *  - teacher leave : student = null, applicant = the teacher
 */
@Entity
@Table(name = "leave_applications")
@Getter
@Setter
public class LeaveApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    /** STUDENT, PARENT or TEACHER */
    @Column(name = "applicant_role", length = 20)
    private String applicantRole;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(length = 500, nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @Column(length = 300)
    private String reviewRemarks;

    // ---- helpers for templates (not stored) ----
    public long getDays() {
        return ChronoUnit.DAYS.between(fromDate, toDate) + 1;
    }

    public String getBadgeClass() {
        switch (status) {
            case APPROVED: return "bg-success";
            case REJECTED: return "bg-danger";
            default:       return "bg-warning text-dark";
        }
    }

    /** Who the leave is for: student's name or teacher's name. */
    public String getPersonName() {
        return student != null ? student.getFullName() : applicant.getFullName();
    }
}

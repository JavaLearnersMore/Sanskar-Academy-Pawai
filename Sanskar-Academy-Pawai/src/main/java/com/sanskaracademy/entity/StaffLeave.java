package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_leave")
public class StaffLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffProfile staff;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(nullable = false)
    private int days;

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StaffLeaveStatus status;

    @Column(name = "admin_note", length = 300)
    private String adminNote;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public StaffProfile getStaff() { return staff; }
    public void setStaff(StaffProfile v) { this.staff = v; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate v) { this.fromDate = v; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate v) { this.toDate = v; }
    public int getDays() { return days; }
    public void setDays(int v) { this.days = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public StaffLeaveStatus getStatus() { return status; }
    public void setStatus(StaffLeaveStatus v) { this.status = v; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String v) { this.adminNote = v; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime v) { this.appliedAt = v; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime v) { this.decidedAt = v; }

    public String getBadgeClass() {
        if (status == StaffLeaveStatus.APPROVED) return "bg-success";
        if (status == StaffLeaveStatus.REJECTED) return "bg-danger";
        if (status == StaffLeaveStatus.CANCELLED) return "bg-secondary";
        return "bg-warning text-dark";
    }

}

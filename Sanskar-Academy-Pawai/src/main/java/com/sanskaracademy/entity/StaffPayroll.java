package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_payroll", uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "pay_month", "pay_year"}))
public class StaffPayroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffProfile staff;

    @Column(name = "pay_month", nullable = false)
    private int payMonth;

    @Column(name = "pay_year", nullable = false)
    private int payYear;

    @Column(name = "working_days")
    private int workingDays;

    @Column(name = "present_days", precision = 6, scale = 1)
    private BigDecimal presentDays;

    @Column(name = "half_days")
    private int halfDays;

    @Column(name = "absent_days")
    private int absentDays;

    @Column(name = "leave_days")
    private int leaveDays;

    @Column(name = "unmarked_days")
    private int unmarkedDays;

    @Column(name = "paid_leave_allowed", precision = 6, scale = 1)
    private BigDecimal paidLeaveAllowed;

    @Column(name = "deduct_days", precision = 6, scale = 1)
    private BigDecimal deductDays;

    @Column(name = "gross_salary", precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "per_day", precision = 12, scale = 2)
    private BigDecimal perDay;

    @Column(name = "leave_deduction", precision = 12, scale = 2)
    private BigDecimal leaveDeduction;

    @Column(name = "other_deduction", precision = 12, scale = 2)
    private BigDecimal otherDeduction;

    @Column(precision = 12, scale = 2)
    private BigDecimal bonus;

    @Column(name = "net_salary", precision = 12, scale = 2)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PayrollStatus status;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(length = 300)
    private String remarks;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public StaffProfile getStaff() { return staff; }
    public void setStaff(StaffProfile v) { this.staff = v; }
    public int getPayMonth() { return payMonth; }
    public void setPayMonth(int v) { this.payMonth = v; }
    public int getPayYear() { return payYear; }
    public void setPayYear(int v) { this.payYear = v; }
    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int v) { this.workingDays = v; }
    public BigDecimal getPresentDays() { return presentDays; }
    public void setPresentDays(BigDecimal v) { this.presentDays = v; }
    public int getHalfDays() { return halfDays; }
    public void setHalfDays(int v) { this.halfDays = v; }
    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int v) { this.absentDays = v; }
    public int getLeaveDays() { return leaveDays; }
    public void setLeaveDays(int v) { this.leaveDays = v; }
    public int getUnmarkedDays() { return unmarkedDays; }
    public void setUnmarkedDays(int v) { this.unmarkedDays = v; }
    public BigDecimal getPaidLeaveAllowed() { return paidLeaveAllowed; }
    public void setPaidLeaveAllowed(BigDecimal v) { this.paidLeaveAllowed = v; }
    public BigDecimal getDeductDays() { return deductDays; }
    public void setDeductDays(BigDecimal v) { this.deductDays = v; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal v) { this.grossSalary = v; }
    public BigDecimal getPerDay() { return perDay; }
    public void setPerDay(BigDecimal v) { this.perDay = v; }
    public BigDecimal getLeaveDeduction() { return leaveDeduction; }
    public void setLeaveDeduction(BigDecimal v) { this.leaveDeduction = v; }
    public BigDecimal getOtherDeduction() { return otherDeduction; }
    public void setOtherDeduction(BigDecimal v) { this.otherDeduction = v; }
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal v) { this.bonus = v; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal v) { this.netSalary = v; }
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus v) { this.status = v; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate v) { this.paidDate = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }

    public String getMonthName() {
        return java.time.Month.of(payMonth).getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + payYear;
    }

}

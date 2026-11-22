package com.sanskaracademy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "work_setting")
public class WorkSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paid_leave_per_month", precision = 4, scale = 1, nullable = false)
    private BigDecimal paidLeavePerMonth;

    @Column(name = "month_divisor", nullable = false)
    private int monthDivisor;

    @Column(name = "weekly_off_days", length = 100)
    private String weeklyOffDays;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getPaidLeavePerMonth() { return paidLeavePerMonth; }
    public void setPaidLeavePerMonth(BigDecimal paidLeavePerMonth) { this.paidLeavePerMonth = paidLeavePerMonth; }
    public int getMonthDivisor() { return monthDivisor; }
    public void setMonthDivisor(int monthDivisor) { this.monthDivisor = monthDivisor; }
    public String getWeeklyOffDays() { return weeklyOffDays; }
    public void setWeeklyOffDays(String weeklyOffDays) { this.weeklyOffDays = weeklyOffDays; }
}

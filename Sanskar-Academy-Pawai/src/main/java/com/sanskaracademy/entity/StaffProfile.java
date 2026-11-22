package com.sanskaracademy.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "staff_profile")
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100)
    private String designation;

    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", length = 20)
    private StaffType staffType;

    @Column(name = "monthly_salary", precision = 12, scale = 2, nullable = false)
    private BigDecimal monthlySalary;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** empty = use the academy setting; otherwise e.g. "MONDAY,THURSDAY" */
    @Column(name = "weekly_off_days", length = 100)
    private String weeklyOffDays;

    @Column(nullable = false)
    private boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public StaffType getStaffType() { return staffType; }
    public void setStaffType(StaffType staffType) { this.staffType = staffType; }
    public BigDecimal getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(BigDecimal monthlySalary) { this.monthlySalary = monthlySalary; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getWeeklyOffDays() { return weeklyOffDays; }
    public void setWeeklyOffDays(String weeklyOffDays) { this.weeklyOffDays = weeklyOffDays; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

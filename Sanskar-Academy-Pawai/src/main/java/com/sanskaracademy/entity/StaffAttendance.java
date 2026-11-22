package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "att_date"}))
public class StaffAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffProfile staff;

    @Column(name = "att_date", nullable = false)
    private LocalDate attDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StaffAttendanceStatus status;

    @Column(length = 200)
    private String remark;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public StaffProfile getStaff() { return staff; }
    public void setStaff(StaffProfile v) { this.staff = v; }
    public LocalDate getAttDate() { return attDate; }
    public void setAttDate(LocalDate v) { this.attDate = v; }
    public StaffAttendanceStatus getStatus() { return status; }
    public void setStatus(StaffAttendanceStatus v) { this.status = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }

}

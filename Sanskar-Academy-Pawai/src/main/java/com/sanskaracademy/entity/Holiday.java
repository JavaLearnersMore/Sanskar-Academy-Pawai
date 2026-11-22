package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "holiday")
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "holiday_date", nullable = false, unique = true)
    private LocalDate holidayDate;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "send_wishes", nullable = false)
    private boolean sendWishes;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate v) { this.holidayDate = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public boolean isSendWishes() { return sendWishes; }
    public void setSendWishes(boolean v) { this.sendWishes = v; }

}

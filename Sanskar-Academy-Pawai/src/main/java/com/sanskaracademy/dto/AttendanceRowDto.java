package com.sanskaracademy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One line of the teacher's attendance sheet. */
@Getter
@AllArgsConstructor
public class AttendanceRowDto {
    private Long studentId;
    private String studentName;
    private String email;
    private String status;     // PRESENT or ABSENT
    private boolean onLeave;   // has an approved leave on that date
}
package com.sanskaracademy.dto;

import com.sanskaracademy.entity.ExamAttempt;
import com.sanskaracademy.entity.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One student's line on the teacher's live exam monitor. state = NOT_STARTED, IN_PROGRESS, SUBMITTED, REVIEW */
@Getter
@AllArgsConstructor
public class MonitorRowDto {
    private User student;
    private ExamAttempt attempt;   // null when not started
    private String state;

    public String getLabel() {
        switch (state) {
            case "IN_PROGRESS": return "Writing now";
            case "SUBMITTED":   return "Submitted";
            case "REVIEW":      return "Needs marking";
            default:            return "Not started";
        }
    }

    public String getBadgeClass() {
        switch (state) {
            case "IN_PROGRESS": return "bg-warning text-dark";
            case "SUBMITTED":   return "bg-success";
            case "REVIEW":      return "bg-info text-dark";
            default:            return "bg-secondary";
        }
    }

    /** minutes:seconds left for a student who is writing */
    public String getTimeLeft() {
        if (attempt == null || !"IN_PROGRESS".equals(state)) return "-";
        long s = attempt.getRemainingSeconds();
        return String.format("%d:%02d", s / 60, s % 60);
    }
}

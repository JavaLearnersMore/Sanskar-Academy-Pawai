package com.sanskaracademy.dto;

import com.sanskaracademy.entity.Exam;
import com.sanskaracademy.entity.ExamAttempt;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** One row of the student's exam list. state = AVAILABLE, IN_PROGRESS, DONE, MISSED */
@Getter
@AllArgsConstructor
public class StudentExamDto {
    private Exam exam;
    private ExamAttempt attempt;   // may be null
    private String state;

    public String getStateLabel() {
        switch (state) {
            case "AVAILABLE":   return "Start now";
            case "IN_PROGRESS": return "In progress";
            case "DONE":        return attempt != null && attempt.isPending() ? "Result pending" : "Completed";
            default:            return "Missed / closed";
        }
    }

    public String getBadgeClass() {
        switch (state) {
            case "AVAILABLE":   return "bg-primary";
            case "IN_PROGRESS": return "bg-warning text-dark";
            case "DONE":        return "bg-success";
            default:            return "bg-secondary";
        }
    }
}

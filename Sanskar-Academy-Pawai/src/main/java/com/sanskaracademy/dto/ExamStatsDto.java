package com.sanskaracademy.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExamStatsDto {

    private int enrolled;        // students in the class
    private int attempted;       // submitted attempts
    private int average;         // average percentage
    private int highest;
    private int lowest;
    private int passed;          // >= 40 %
    private List<QuestionStat> questionStats;

    @Getter
    @AllArgsConstructor
    public static class QuestionStat {
        private Long questionId;
        private int number;
        private String text;
        private int percent;     // % of available marks obtained by those who answered
        private int answered;

        public String getBarClass() {
            if (percent >= 70) return "bg-success";
            if (percent >= 40) return "bg-warning";
            return "bg-danger";
        }
    }
}

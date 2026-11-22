package com.sanskaracademy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** The teacher's view of how strong one student is. */
@Getter
@AllArgsConstructor
public class StudentLevelDto {

    private Long studentId;
    private String studentName;
    private String className;
    private int examsTaken;
    private int averagePercent;
    private int lastPercent;
    private String trend;          // UP, DOWN, STEADY, NEW, NONE

    public String getLevel() {
        return levelFor(examsTaken, averagePercent);
    }

    public String getBadgeClass() {
        switch (getLevel()) {
            case "Advanced":      return "bg-success";
            case "Intermediate":  return "bg-primary";
            case "Needs support": return "bg-danger";
            default:              return "bg-secondary";
        }
    }

    public String getTrendLabel() {
        switch (trend) {
            case "UP":     return "⬆ Improving";
            case "DOWN":   return "⬇ Dropping";
            case "STEADY": return "➡ Steady";
            case "NEW":    return "🆕 First test";
            default:       return "-";
        }
    }

    public static String levelFor(int examsTaken, int avg) {
        if (examsTaken == 0) return "No data";
        if (avg >= 75) return "Advanced";
        if (avg >= 50) return "Intermediate";
        return "Needs support";
    }
}

package com.sanskaracademy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeSummaryDto {

    private Long studentId;
    private String studentName;
    private String email;
    private String mobile;

    private String academicYear;
    private LocalDate dueDate;

    private BigDecimal totalFee;
    private BigDecimal discount;
    private BigDecimal payable;   // totalFee - discount
    private BigDecimal paid;
    private BigDecimal pending;

    private int percentPaid;
    private boolean overdue;

    /** NOT_SET, UNPAID, PARTIAL, PAID */
    private String status;

    public String getStatusLabel() {
        switch (status) {
            case "PAID":    return "Paid";
            case "PARTIAL": return "Partial";
            case "UNPAID":  return "Unpaid";
            default:        return "Not Set";
        }
    }

    public String getBadgeClass() {
        switch (status) {
            case "PAID":    return "bg-success";
            case "PARTIAL": return "bg-warning text-dark";
            case "UNPAID":  return "bg-danger";
            default:        return "bg-secondary";
        }
    }
}

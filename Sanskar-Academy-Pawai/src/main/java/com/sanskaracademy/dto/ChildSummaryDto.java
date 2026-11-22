package com.sanskaracademy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** What the parent dashboard shows for one child. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildSummaryDto {
    private Long studentId;
    private String studentName;
    private String className;
    private FeeSummaryDto fee;
    private long presentCount;
    private long absentCount;
    private long attendancePercent;
}

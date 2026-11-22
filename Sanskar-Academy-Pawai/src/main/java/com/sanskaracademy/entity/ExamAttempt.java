package com.sanskaracademy.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** One student's single attempt at one exam. */
@Entity
@Table(name = "exam_attempts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"}))
@Getter
@Setter
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private int score;

    @Column(name = "total_marks")
    private int totalMarks;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExamAnswer> answers = new ArrayList<>();

    // ---- helpers for templates (not stored) ----
    public int getPercentage() {
        return totalMarks > 0 ? (int) Math.round(score * 100.0 / totalMarks) : 0;
    }

    public boolean isPending() {
        return status == AttemptStatus.PENDING_REVIEW;
    }

    public String getGrade() {
        int p = getPercentage();
        if (p >= 90) return "A+";
        if (p >= 75) return "A";
        if (p >= 60) return "B";
        if (p >= 40) return "C";
        return "D";
    }

    public String getBadgeClass() {
        int p = getPercentage();
        if (isPending()) return "bg-secondary";
        if (p >= 75) return "bg-success";
        if (p >= 40) return "bg-warning text-dark";
        return "bg-danger";
    }

    public boolean isPassed() {
        return getPercentage() >= 40;
    }

    /** seconds left to finish an IN_PROGRESS attempt */
    public long getRemainingSeconds() {
        LocalDateTime end = startedAt.plusMinutes(exam.getDurationMinutes());
        return Math.max(0, Duration.between(LocalDateTime.now(), end).getSeconds());
    }

    public boolean isExpired() {
        return status == AttemptStatus.IN_PROGRESS && getRemainingSeconds() == 0;
    }
}

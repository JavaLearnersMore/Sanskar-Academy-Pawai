package com.sanskaracademy.entity;

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
import lombok.Getter;
import lombok.Setter;

/** A test created by a teacher for one subject (and therefore one class). */
@Entity
@Table(name = "exams")
@Getter
@Setter
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 500)
    private String description;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    /** students can start the exam until this time (optional) */
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status = ExamStatus.DRAFT;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExamQuestion> questions = new ArrayList<>();

    // ---- helpers for templates (not stored) ----
    public int getTotalMarks() {
        int t = 0;
        for (ExamQuestion q : questions) t += q.getMarks();
        return t;
    }

    public boolean isOpenForStart() {
        return status == ExamStatus.PUBLISHED && (dueDate == null || LocalDateTime.now().isBefore(dueDate));
    }

    public String getStatusBadge() {
        switch (status) {
            case PUBLISHED: return "bg-success";
            case CLOSED:    return "bg-secondary";
            default:        return "bg-warning text-dark";
        }
    }
}

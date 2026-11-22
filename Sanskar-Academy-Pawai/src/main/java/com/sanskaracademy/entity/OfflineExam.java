package com.sanskaracademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "offline_exam")
public class OfflineExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    @Column(name = "exam_date")
    private LocalDate examDate;

    @Column(name = "results_published", nullable = false)
    private boolean resultsPublished;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Exam getExam() { return exam; }
    public void setExam(Exam v) { this.exam = v; }
    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate v) { this.examDate = v; }
    public boolean isResultsPublished() { return resultsPublished; }
    public void setResultsPublished(boolean v) { this.resultsPublished = v; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime v) { this.publishedAt = v; }

}

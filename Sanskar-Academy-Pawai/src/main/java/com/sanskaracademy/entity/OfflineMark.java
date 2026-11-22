package com.sanskaracademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "offline_mark", uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"}))
public class OfflineMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column
    private Integer marks;

    @Column(length = 300)
    private String remarks;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Exam getExam() { return exam; }
    public void setExam(Exam v) { this.exam = v; }
    public User getStudent() { return student; }
    public void setStudent(User v) { this.student = v; }
    public Integer getMarks() { return marks; }
    public void setMarks(Integer v) { this.marks = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

}

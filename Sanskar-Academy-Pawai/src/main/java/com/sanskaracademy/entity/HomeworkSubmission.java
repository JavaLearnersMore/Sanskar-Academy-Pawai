package com.sanskaracademy.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "homework_submission", uniqueConstraints = @UniqueConstraint(name = "uk_hw_student", columnNames = {"homework_id", "student_id"}))
public class HomeworkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "homework_id", nullable = false)
    private Homework homework;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    private LocalDateTime submittedAt;

    @Column(length = 2000)
    private String answerText;

    @Column(length = 255)
    private String filePath;

    @Column(length = 160)
    private String fileName;

    private Integer marks;

    @Column(length = 250)
    private String remark;

    private LocalDateTime checkedAt;

    private boolean late;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Homework getHomework() { return homework; }
    public void setHomework(Homework homework) { this.homework = homework; }
    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    public boolean isLate() { return late; }
    public void setLate(boolean late) { this.late = late; }
}

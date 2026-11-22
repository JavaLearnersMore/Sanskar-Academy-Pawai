package com.sanskaracademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One lecture on one real date (made from the weekly pattern or added as an extra class). */
@Entity
@Table(name = "lecture_session",
       uniqueConstraints = @UniqueConstraint(name = "uk_session_slot", columnNames = {"subject_id", "lecture_date", "start_time"}),
       indexes = @Index(name = "idx_session_date", columnList = "lecture_date"))
public class LectureSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "lecture_date", nullable = false)
    private LocalDate lectureDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 60)
    private String room;

    /** teacher who takes this lecture instead of the subject's teacher (null = the planned teacher) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_teacher_id")
    private User substituteTeacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LectureStatus status = LectureStatus.PLANNED;

    @Column(length = 250)
    private String topic;

    @Column(length = 250)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by")
    private User markedBy;

    private LocalDateTime markedAt;

    /** true when the lecture was added by hand (extra class), false when generated from the weekly pattern */
    @Column(nullable = false)
    private boolean extra;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public LocalDate getLectureDate() { return lectureDate; }
    public void setLectureDate(LocalDate lectureDate) { this.lectureDate = lectureDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    public User getSubstituteTeacher() { return substituteTeacher; }
    public void setSubstituteTeacher(User substituteTeacher) { this.substituteTeacher = substituteTeacher; }
    public LectureStatus getStatus() { return status; }
    public void setStatus(LectureStatus status) { this.status = status; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public User getMarkedBy() { return markedBy; }
    public void setMarkedBy(User markedBy) { this.markedBy = markedBy; }
    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
    public boolean isExtra() { return extra; }
    public void setExtra(boolean extra) { this.extra = extra; }
}

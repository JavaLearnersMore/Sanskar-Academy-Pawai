package com.sanskaracademy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** The answer a student gave to one question of an attempt. */
@Entity
@Table(name = "exam_answers")
@Getter
@Setter
public class ExamAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuestion question;

    /** A/B/C/D for MCQ */
    @Column(name = "selected_option", length = 1)
    private String selectedOption;

    /** written answer for SHORT */
    @Column(name = "answer_text", length = 2000)
    private String answerText;

    /** null = waiting for teacher review */
    @Column(name = "marks_awarded")
    private Integer marksAwarded;

    private Boolean correct;

    @Column(length = 1000)
    private String feedback;

    @Column(name = "teacher_graded")
    private boolean teacherGraded;

    public boolean isFullMarks() {
        return marksAwarded != null && marksAwarded == question.getMarks();
    }

    public String getRowClass() {
        if (marksAwarded == null) return "border-secondary";
        if (marksAwarded == question.getMarks()) return "border-success";
        if (marksAwarded == 0) return "border-danger";
        return "border-warning";
    }
}

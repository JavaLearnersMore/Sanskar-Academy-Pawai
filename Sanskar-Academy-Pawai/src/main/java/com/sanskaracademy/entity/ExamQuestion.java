package com.sanskaracademy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "exam_questions")
@Getter
@Setter
public class ExamQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type = QuestionType.MCQ;

    @Column(length = 300) private String optionA;
    @Column(length = 300) private String optionB;
    @Column(length = 300) private String optionC;
    @Column(length = 300) private String optionD;

    /** A, B, C or D (MCQ only) */
    @Column(name = "correct_option", length = 1)
    private String correctOption;

    /** teacher's model answer (SHORT only) - used by AI / teacher for marking */
    @Column(name = "model_answer", length = 1000)
    private String modelAnswer;

    @Column(nullable = false)
    private int marks = 1;

    /** text of the option with the given letter */
    public String optionText(String letter) {
        if (letter == null) return null;
        switch (letter) {
            case "A": return optionA;
            case "B": return optionB;
            case "C": return optionC;
            case "D": return optionD;
            default:  return null;
        }
    }
}

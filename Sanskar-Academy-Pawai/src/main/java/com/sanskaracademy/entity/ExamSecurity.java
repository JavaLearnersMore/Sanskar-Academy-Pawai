package com.sanskaracademy.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_security")
public class ExamSecurity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    @Column(name = "strict_mode", nullable = false)
    private boolean strictMode;

    @Column(name = "max_warnings", nullable = false)
    private int maxWarnings;

    @Column(name = "block_copy", nullable = false)
    private boolean blockCopy;

    @Column(nullable = false)
    private boolean shuffle;

    @Column(name = "pool_size")
    private Integer poolSize;

    @Column(name = "per_question_seconds")
    private Integer perQuestionSeconds;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Exam getExam() { return exam; }
    public void setExam(Exam v) { this.exam = v; }
    public boolean isStrictMode() { return strictMode; }
    public void setStrictMode(boolean v) { this.strictMode = v; }
    public int getMaxWarnings() { return maxWarnings; }
    public void setMaxWarnings(int v) { this.maxWarnings = v; }
    public boolean isBlockCopy() { return blockCopy; }
    public void setBlockCopy(boolean v) { this.blockCopy = v; }
    public boolean isShuffle() { return shuffle; }
    public void setShuffle(boolean v) { this.shuffle = v; }
    public Integer getPoolSize() { return poolSize; }
    public void setPoolSize(Integer v) { this.poolSize = v; }
    public Integer getPerQuestionSeconds() { return perQuestionSeconds; }
    public void setPerQuestionSeconds(Integer v) { this.perQuestionSeconds = v; }

}

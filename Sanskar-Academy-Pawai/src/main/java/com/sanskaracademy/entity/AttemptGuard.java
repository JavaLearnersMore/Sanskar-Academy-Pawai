package com.sanskaracademy.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_guard")
public class AttemptGuard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attempt_id", nullable = false, unique = true)
    private ExamAttempt attempt;

    @Column(nullable = false)
    private int violations;

    @Column(name = "last_violation_at")
    private LocalDateTime lastViolationAt;

    @Column(name = "auto_submitted", nullable = false)
    private boolean autoSubmitted;

    @Column(name = "question_order", columnDefinition = "TEXT")
    private String questionOrder;

    @Column(name = "option_orders", columnDefinition = "TEXT")
    private String optionOrders;

    @Column(name = "q_index", nullable = false)
    private int qIndex;

    @Column(name = "q_started_at")
    private LocalDateTime qStartedAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public ExamAttempt getAttempt() { return attempt; }
    public void setAttempt(ExamAttempt v) { this.attempt = v; }
    public int getViolations() { return violations; }
    public void setViolations(int v) { this.violations = v; }
    public LocalDateTime getLastViolationAt() { return lastViolationAt; }
    public void setLastViolationAt(LocalDateTime v) { this.lastViolationAt = v; }
    public boolean isAutoSubmitted() { return autoSubmitted; }
    public void setAutoSubmitted(boolean v) { this.autoSubmitted = v; }
    public String getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(String v) { this.questionOrder = v; }
    public String getOptionOrders() { return optionOrders; }
    public void setOptionOrders(String v) { this.optionOrders = v; }
    public int getQIndex() { return qIndex; }
    public void setQIndex(int v) { this.qIndex = v; }
    public LocalDateTime getQStartedAt() { return qStartedAt; }
    public void setQStartedAt(LocalDateTime v) { this.qStartedAt = v; }

}

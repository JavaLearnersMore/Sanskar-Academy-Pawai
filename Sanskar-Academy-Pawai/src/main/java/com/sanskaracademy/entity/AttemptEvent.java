package com.sanskaracademy.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_event", indexes = @Index(name = "idx_attempt_event_attempt", columnList = "attempt_id"))
public class AttemptEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamAttempt attempt;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime at;

    @Column(length = 120)
    private String detail;

    @Column(nullable = false)
    private boolean counted;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public ExamAttempt getAttempt() { return attempt; }
    public void setAttempt(ExamAttempt v) { this.attempt = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public LocalDateTime getAt() { return at; }
    public void setAt(LocalDateTime v) { this.at = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { this.detail = v; }
    public boolean isCounted() { return counted; }
    public void setCounted(boolean v) { this.counted = v; }

    public String getLabel() {
        switch (type) {
            case "TAB_HIDDEN": return "Left the exam tab";
            case "WINDOW_BLUR": return "Switched to another window";
            case "FULLSCREEN_EXIT": return "Left full screen";
            case "COPY_BLOCKED": return "Tried to copy / cut";
            case "PASTE_BLOCKED": return "Tried to paste";
            case "MENU_BLOCKED": return "Right-click / shortcut blocked";
            case "FULLSCREEN_DENIED": return "Full screen not allowed by browser";
            default: return type;
        }
    }

}

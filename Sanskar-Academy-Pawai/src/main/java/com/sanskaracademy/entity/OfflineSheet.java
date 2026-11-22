package com.sanskaracademy.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "offline_sheet")
public class OfflineSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mark_id", nullable = false)
    private OfflineMark mark;

    @Column(name = "stored_name", nullable = false, length = 80)
    private String storedName;

    @Column(name = "original_name", length = 150)
    private String originalName;

    @Column(name = "content_type", length = 40)
    private String contentType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public OfflineMark getMark() { return mark; }
    public void setMark(OfflineMark v) { this.mark = v; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String v) { this.storedName = v; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String v) { this.originalName = v; }
    public String getContentType() { return contentType; }
    public void setContentType(String v) { this.contentType = v; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime v) { this.uploadedAt = v; }

    public boolean isImage() { return contentType != null && contentType.startsWith("image/"); }

}

package com.aitutor.backend.entity;

import com.aitutor.backend.document.MaterialStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "materials")
public class Material {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private LearningProject project;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_path", nullable = false, length = 2048)
    private String storagePath;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MaterialStatus status;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "extracted_char_count", nullable = false)
    private int extractedCharCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Material() {
    }

    public Material(
            UUID id,
            LearningProject project,
            String originalFilename,
            String storagePath,
            long fileSizeBytes
    ) {
        this.id = id;
        this.project = project;
        this.originalFilename = originalFilename;
        this.storagePath = storagePath;
        this.fileSizeBytes = fileSizeBytes;
        this.status = MaterialStatus.PROCESSING;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markProcessing() {
        status = MaterialStatus.PROCESSING;
        errorMessage = null;
    }

    public void markProcessed(int pageCount, int extractedCharCount) {
        this.status = MaterialStatus.PROCESSED;
        this.pageCount = pageCount;
        this.extractedCharCount = extractedCharCount;
        this.errorMessage = null;
    }

    public void markFailed(String message) {
        this.status = MaterialStatus.FAILED;
        this.errorMessage = message == null ? "The PDF could not be processed." : message;
    }

    public UUID getId() {
        return id;
    }

    public LearningProject getProject() {
        return project;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public MaterialStatus getStatus() {
        return status;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getExtractedCharCount() {
        return extractedCharCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

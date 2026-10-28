package com.aitutor.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "assessment_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_assessment_attempt_number",
                columnNames = {"assessment_id", "attempt_number"}
        )
)
public class AssessmentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "score_percent", nullable = false)
    private int scorePercent;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected AssessmentAttempt() {
    }

    public AssessmentAttempt(Assessment assessment, int attemptNumber, int scorePercent) {
        this.assessment = assessment;
        this.attemptNumber = attemptNumber;
        this.scorePercent = scorePercent;
    }

    @PrePersist
    void onCreate() {
        submittedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public int getScorePercent() {
        return scorePercent;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}

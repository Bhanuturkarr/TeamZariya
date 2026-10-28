package com.aitutor.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learning_plans")
public class LearningPlan {

    public enum Status { ACTIVE, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private LearningProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_attempt_id", nullable = false)
    private AssessmentAttempt assessmentAttempt;

    @Column(name = "available_study_minutes", nullable = false)
    private int availableStudyMinutes;

    @Column(name = "learning_preference", length = 500)
    private String learningPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LearningPlan() {
    }

    public LearningPlan(
            LearningProject project,
            Material material,
            Assessment assessment,
            AssessmentAttempt assessmentAttempt,
            int availableStudyMinutes,
            String learningPreference
    ) {
        this.project = project;
        this.material = material;
        this.assessment = assessment;
        this.assessmentAttempt = assessmentAttempt;
        this.availableStudyMinutes = availableStudyMinutes;
        this.learningPreference = learningPreference;
        this.status = Status.ACTIVE;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void complete() { status = Status.COMPLETED; }
    public void activate() { status = Status.ACTIVE; }
    public UUID getId() { return id; }
    public LearningProject getProject() { return project; }
    public Material getMaterial() { return material; }
    public Assessment getAssessment() { return assessment; }
    public AssessmentAttempt getAssessmentAttempt() { return assessmentAttempt; }
    public int getAvailableStudyMinutes() { return availableStudyMinutes; }
    public String getLearningPreference() { return learningPreference; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}

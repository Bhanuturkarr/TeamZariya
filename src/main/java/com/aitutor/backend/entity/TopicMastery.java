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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topic_mastery", uniqueConstraints = @UniqueConstraint(
        name = "uq_topic_mastery_project_name", columnNames = {"project_id", "topic_name"}
))
public class TopicMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private LearningProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "topic_name", nullable = false, length = 160)
    private String topicName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostic_attempt_id")
    private AssessmentAttempt diagnosticAttempt;

    @Column(name = "diagnostic_attempts", nullable = false)
    private int diagnosticAttempts;

    @Column(name = "diagnostic_correct", nullable = false)
    private int diagnosticCorrect;

    @Column(name = "practice_attempts", nullable = false)
    private int practiceAttempts;

    @Column(name = "practice_correct", nullable = false)
    private int practiceCorrect;

    @Column(name = "mastery_score", nullable = false)
    private int masteryScore;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TopicMastery() {
    }

    public TopicMastery(LearningProject project, Topic topic, String topicName) {
        this.project = project;
        this.topic = topic;
        this.topicName = topicName;
        this.masteryScore = 50;
    }

    public void setDiagnostic(AssessmentAttempt attempt, int correct, int total) {
        if (diagnosticAttempt != null && diagnosticAttempt.getId().equals(attempt.getId())) {
            return;
        }
        diagnosticAttempt = attempt;
        diagnosticCorrect = correct;
        diagnosticAttempts = total;
        recalculate();
    }

    public void recordPractice(boolean correct) {
        practiceAttempts++;
        if (correct) practiceCorrect++;
        recalculate();
    }

    private void recalculate() {
        int total = diagnosticAttempts + practiceAttempts;
        int correct = diagnosticCorrect + practiceCorrect;
        masteryScore = total == 0 ? 50 : Math.round(correct * 100.0f / total);
    }

    @PrePersist
    @PreUpdate
    void onSave() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public LearningProject getProject() { return project; }
    public Topic getTopic() { return topic; }
    public String getTopicName() { return topicName; }
    public AssessmentAttempt getDiagnosticAttempt() { return diagnosticAttempt; }
    public int getDiagnosticAttempts() { return diagnosticAttempts; }
    public int getDiagnosticCorrect() { return diagnosticCorrect; }
    public int getPracticeAttempts() { return practiceAttempts; }
    public int getPracticeCorrect() { return practiceCorrect; }
    public int getMasteryScore() { return masteryScore; }
    public Instant getUpdatedAt() { return updatedAt; }
}

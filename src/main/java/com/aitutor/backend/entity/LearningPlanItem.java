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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "learning_plan_items", uniqueConstraints = @UniqueConstraint(
        name = "uq_learning_plan_item_sequence", columnNames = {"plan_id", "sequence_number"}
))
public class LearningPlanItem {

    public enum Status { PLANNED, IN_PROGRESS, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private LearningPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "topic_name", nullable = false, length = 160)
    private String topicName;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "estimated_minutes", nullable = false)
    private int estimatedMinutes;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    protected LearningPlanItem() {
    }

    public LearningPlanItem(
            LearningPlan plan,
            Topic topic,
            String topicName,
            int sequenceNumber,
            int estimatedMinutes,
            String rationale
    ) {
        this.plan = plan;
        this.topic = topic;
        this.topicName = topicName;
        this.sequenceNumber = sequenceNumber;
        this.estimatedMinutes = estimatedMinutes;
        this.rationale = rationale;
        this.status = Status.PLANNED;
    }

    public void start() { if (status == Status.PLANNED) status = Status.IN_PROGRESS; }
    public void resume() { status = Status.IN_PROGRESS; }
    public void complete() { status = Status.COMPLETED; }
    public UUID getId() { return id; }
    public LearningPlan getPlan() { return plan; }
    public Topic getTopic() { return topic; }
    public String getTopicName() { return topicName; }
    public int getSequenceNumber() { return sequenceNumber; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public String getRationale() { return rationale; }
    public Status getStatus() { return status; }
}

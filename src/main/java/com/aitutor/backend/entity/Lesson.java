package com.aitutor.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_item_id", nullable = false)
    private LearningPlanItem planItem;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String objective;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "worked_example", nullable = false, columnDefinition = "TEXT")
    private String workedExample;

    @Column(name = "source_page", nullable = false)
    private int sourcePage;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Lesson() {
    }

    public Lesson(
            LearningPlanItem planItem,
            String title,
            String objective,
            String explanation,
            String workedExample,
            int sourcePage
    ) {
        this.planItem = planItem;
        this.title = title;
        this.objective = objective;
        this.explanation = explanation;
        this.workedExample = workedExample;
        this.sourcePage = sourcePage;
    }

    public void complete() {
        if (!completed) {
            completed = true;
            completedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public LearningPlanItem getPlanItem() { return planItem; }
    public String getTitle() { return title; }
    public String getObjective() { return objective; }
    public String getExplanation() { return explanation; }
    public String getWorkedExample() { return workedExample; }
    public int getSourcePage() { return sourcePage; }
    public boolean isCompleted() { return completed; }
    public Instant getCompletedAt() { return completedAt; }
}

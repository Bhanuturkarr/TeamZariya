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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exercise_attempts")
public class ExerciseAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "selected_option_index", nullable = false)
    private int selectedOptionIndex;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "mastery_before", nullable = false)
    private int masteryBefore;

    @Column(name = "mastery_after", nullable = false)
    private int masteryAfter;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected ExerciseAttempt() {
    }

    public ExerciseAttempt(Exercise exercise, int selectedOptionIndex, boolean correct,
                           int masteryBefore, int masteryAfter) {
        this.exercise = exercise;
        this.selectedOptionIndex = selectedOptionIndex;
        this.correct = correct;
        this.masteryBefore = masteryBefore;
        this.masteryAfter = masteryAfter;
    }

    @PrePersist
    void onCreate() { submittedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Exercise getExercise() { return exercise; }
    public int getSelectedOptionIndex() { return selectedOptionIndex; }
    public boolean isCorrect() { return correct; }
    public int getMasteryBefore() { return masteryBefore; }
    public int getMasteryAfter() { return masteryAfter; }
    public Instant getSubmittedAt() { return submittedAt; }
}

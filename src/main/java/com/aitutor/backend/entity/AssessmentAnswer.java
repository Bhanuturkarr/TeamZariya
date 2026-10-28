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
        name = "assessment_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_assessment_answer_question",
                columnNames = {"attempt_id", "question_id"}
        )
)
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(name = "selected_option_index", nullable = false)
    private int selectedOptionIndex;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt;

    protected AssessmentAnswer() {
    }

    public AssessmentAnswer(
            AssessmentAttempt attempt,
            AssessmentQuestion question,
            int selectedOptionIndex,
            boolean correct
    ) {
        this.attempt = attempt;
        this.question = question;
        this.selectedOptionIndex = selectedOptionIndex;
        this.correct = correct;
    }

    @PrePersist
    void onCreate() {
        answeredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AssessmentAttempt getAttempt() {
        return attempt;
    }

    public AssessmentQuestion getQuestion() {
        return question;
    }

    public int getSelectedOptionIndex() {
        return selectedOptionIndex;
    }

    public boolean isCorrect() {
        return correct;
    }

    public Instant getAnsweredAt() {
        return answeredAt;
    }
}

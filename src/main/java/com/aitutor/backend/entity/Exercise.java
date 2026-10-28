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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "question_text", nullable = false, length = 1000)
    private String questionText;

    @Column(name = "option_a", nullable = false, columnDefinition = "TEXT")
    private String optionA;
    @Column(name = "option_b", nullable = false, columnDefinition = "TEXT")
    private String optionB;
    @Column(name = "option_c", nullable = false, columnDefinition = "TEXT")
    private String optionC;
    @Column(name = "option_d", nullable = false, columnDefinition = "TEXT")
    private String optionD;

    @Column(name = "correct_option_index", nullable = false)
    private int correctOptionIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false, length = 8)
    private String difficulty;

    @Column(name = "source_page", nullable = false)
    private int sourcePage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Exercise() {
    }

    public Exercise(Lesson lesson, String question, List<String> options, int correctOptionIndex,
                    String explanation, String difficulty, int sourcePage) {
        this.lesson = lesson;
        this.questionText = question;
        this.optionA = options.get(0);
        this.optionB = options.get(1);
        this.optionC = options.get(2);
        this.optionD = options.get(3);
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.sourcePage = sourcePage;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Lesson getLesson() { return lesson; }
    public String getQuestionText() { return questionText; }
    public List<String> getOptions() { return List.of(optionA, optionB, optionC, optionD); }
    public String getOption(int index) { return getOptions().get(index); }
    public int getCorrectOptionIndex() { return correctOptionIndex; }
    public String getExplanation() { return explanation; }
    public String getDifficulty() { return difficulty; }
    public int getSourcePage() { return sourcePage; }
    public Instant getCreatedAt() { return createdAt; }
}

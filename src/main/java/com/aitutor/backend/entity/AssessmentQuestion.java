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
import jakarta.persistence.UniqueConstraint;

import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "assessment_questions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_assessment_question_order",
                columnNames = {"assessment_id", "question_order"}
        )
)
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "topic_name", nullable = false, length = 160)
    private String topicName;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

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

    protected AssessmentQuestion() {
    }

    public AssessmentQuestion(
            Assessment assessment,
            Topic topic,
            String topicName,
            int questionOrder,
            String questionText,
            List<String> options,
            int correctOptionIndex,
            String explanation,
            String difficulty,
            int sourcePage
    ) {
        this.assessment = assessment;
        this.topic = topic;
        this.topicName = topicName;
        this.questionOrder = questionOrder;
        this.questionText = questionText;
        this.optionA = options.get(0);
        this.optionB = options.get(1);
        this.optionC = options.get(2);
        this.optionD = options.get(3);
        this.correctOptionIndex = correctOptionIndex;
        this.explanation = explanation;
        this.difficulty = difficulty;
        this.sourcePage = sourcePage;
    }

    public UUID getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public Topic getTopic() {
        return topic;
    }

    public String getTopicName() {
        return topicName;
    }

    public int getQuestionOrder() {
        return questionOrder;
    }

    public String getQuestionText() {
        return questionText;
    }

    public List<String> getOptions() {
        return List.of(optionA, optionB, optionC, optionD);
    }

    public String getOption(int index) {
        return getOptions().get(index);
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getSourcePage() {
        return sourcePage;
    }
}

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
@Table(name = "topic_prerequisites", uniqueConstraints = @UniqueConstraint(
        name = "uq_topic_prerequisite", columnNames = {"topic_id", "prerequisite_topic_id"}
))
public class TopicPrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisite_topic_id", nullable = false)
    private Topic prerequisiteTopic;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TopicPrerequisite() {
    }

    public TopicPrerequisite(Topic topic, Topic prerequisiteTopic, String source) {
        this.topic = topic;
        this.prerequisiteTopic = prerequisiteTopic;
        this.source = source;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Topic getTopic() { return topic; }
    public Topic getPrerequisiteTopic() { return prerequisiteTopic; }
    public String getSource() { return source; }
}

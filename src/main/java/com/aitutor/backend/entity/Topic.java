package com.aitutor.backend.entity;

import com.aitutor.backend.document.TopicExtractionMethod;
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
@Table(
        name = "topics",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_topics_material_name",
                columnNames = {"material_id", "name"}
        )
)
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "source_page", nullable = false)
    private int sourcePage;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_method", nullable = false, length = 24)
    private TopicExtractionMethod extractionMethod;

    protected Topic() {
    }

    public Topic(Material material, String name, int sourcePage, TopicExtractionMethod extractionMethod) {
        this.material = material;
        this.name = name;
        this.sourcePage = sourcePage;
        this.extractionMethod = extractionMethod;
    }

    public UUID getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public int getSourcePage() {
        return sourcePage;
    }

    public TopicExtractionMethod getExtractionMethod() {
        return extractionMethod;
    }
}

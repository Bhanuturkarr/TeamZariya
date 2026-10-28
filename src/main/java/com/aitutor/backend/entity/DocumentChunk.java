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

import java.util.UUID;

@Entity
@Table(
        name = "document_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_document_chunks_material_index",
                columnNames = {"material_id", "chunk_index"}
        )
)
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    protected DocumentChunk() {
    }

    public DocumentChunk(Material material, int chunkIndex, int pageNumber, String content) {
        this.material = material;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.content = content;
    }

    public UUID getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getContent() {
        return content;
    }
}

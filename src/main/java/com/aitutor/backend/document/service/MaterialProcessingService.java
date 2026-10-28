package com.aitutor.backend.document.service;

import com.aitutor.backend.document.exception.MaterialNotFoundException;
import com.aitutor.backend.document.exception.MaterialProcessingException;
import com.aitutor.backend.entity.DocumentChunk;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.entity.Topic;
import com.aitutor.backend.repository.DocumentChunkRepository;
import com.aitutor.backend.repository.MaterialRepository;
import com.aitutor.backend.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MaterialProcessingService {

    private final MaterialRepository materialRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TopicRepository topicRepository;
    private final MaterialStorage storage;
    private final PdfContentExtractor pdfContentExtractor;
    private final TextChunker textChunker;
    private final DeterministicTopicExtractor topicExtractor;

    public MaterialProcessingService(
            MaterialRepository materialRepository,
            DocumentChunkRepository chunkRepository,
            TopicRepository topicRepository,
            MaterialStorage storage,
            PdfContentExtractor pdfContentExtractor,
            TextChunker textChunker,
            DeterministicTopicExtractor topicExtractor
    ) {
        this.materialRepository = materialRepository;
        this.chunkRepository = chunkRepository;
        this.topicRepository = topicRepository;
        this.storage = storage;
        this.pdfContentExtractor = pdfContentExtractor;
        this.textChunker = textChunker;
        this.topicExtractor = topicExtractor;
    }

    @Transactional
    public Material process(UUID materialId) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialNotFoundException(materialId));
        chunkRepository.deleteAllByMaterial_Id(materialId);
        topicRepository.deleteAllByMaterial_Id(materialId);
        chunkRepository.flush();
        topicRepository.flush();

        try {
            PdfExtraction extraction = pdfContentExtractor.extract(storage.resolve(material.getStoragePath()));
            List<TextChunk> chunks = textChunker.chunk(extraction.pages());
            List<TopicCandidate> topics = topicExtractor.extract(extraction.pages());

            chunkRepository.saveAll(chunks.stream()
                    .map(chunk -> new DocumentChunk(
                            material,
                            chunk.chunkIndex(),
                            chunk.pageNumber(),
                            chunk.content()
                    ))
                    .toList());
            topicRepository.saveAll(topics.stream()
                    .map(topic -> new Topic(
                            material,
                            topic.name(),
                            topic.sourcePage(),
                            topic.extractionMethod()
                    ))
                    .toList());
            material.markProcessed(extraction.pageCount(), extraction.extractedCharCount());
        } catch (MaterialProcessingException exception) {
            material.markFailed(exception.getMessage());
        }
        return material;
    }
}

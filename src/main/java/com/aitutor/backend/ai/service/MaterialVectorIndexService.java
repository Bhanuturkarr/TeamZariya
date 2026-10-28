package com.aitutor.backend.ai.service;

import com.aitutor.backend.entity.DocumentChunk;
import com.aitutor.backend.repository.DocumentChunkRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MaterialVectorIndexService {

    private final DocumentChunkRepository chunkRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final Map<UUID, List<String>> indexedChunkIds = new ConcurrentHashMap<>();

    public MaterialVectorIndexService(
            DocumentChunkRepository chunkRepository,
            ObjectProvider<VectorStore> vectorStoreProvider
    ) {
        this.chunkRepository = chunkRepository;
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public boolean isAvailable() {
        return vectorStoreProvider.getIfAvailable() != null;
    }

    public synchronized void indexIfNeeded(UUID materialId) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return;
        }

        List<DocumentChunk> chunks = chunkRepository.findAllByMaterial_IdOrderByChunkIndexAsc(materialId);
        List<String> currentIds = chunks.stream().map(chunk -> chunk.getId().toString()).toList();
        List<String> previousIds = indexedChunkIds.get(materialId);
        if (currentIds.isEmpty() || currentIds.equals(previousIds)) {
            return;
        }

        Set<String> obsoleteIds = new HashSet<>(currentIds);
        if (previousIds != null) {
            obsoleteIds.addAll(previousIds);
        }
        vectorStore.delete(List.copyOf(obsoleteIds));
        vectorStore.add(chunks.stream().map(this::toVectorDocument).toList());
        indexedChunkIds.put(materialId, currentIds);
    }

    private Document toVectorDocument(DocumentChunk chunk) {
        return Document.builder()
                .id(chunk.getId().toString())
                .text(chunk.getContent())
                .metadata(Map.of(
                        "materialId", chunk.getMaterial().getId().toString(),
                        "chunkIndex", chunk.getChunkIndex(),
                        "pageNumber", chunk.getPageNumber()
                ))
                .build();
    }
}

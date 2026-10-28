package com.aitutor.backend.ai.service;

import com.aitutor.backend.ai.model.RetrievalResult;
import com.aitutor.backend.ai.model.RetrievedChunk;
import com.aitutor.backend.entity.DocumentChunk;
import com.aitutor.backend.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RelevantChunkRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelevantChunkRetriever.class);
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]{3,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "also", "and", "are", "because", "been", "between", "but", "can",
            "does", "each", "for", "from", "has", "have", "how", "into", "its", "more", "most",
            "not", "of", "our", "such", "that", "the", "their", "there", "these", "this", "those",
            "through", "was", "were", "what", "when", "where", "which", "who", "why", "will", "with"
    );

    private final DocumentChunkRepository chunkRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final MaterialVectorIndexService vectorIndexService;

    public RelevantChunkRetriever(
            DocumentChunkRepository chunkRepository,
            ObjectProvider<VectorStore> vectorStoreProvider,
            MaterialVectorIndexService vectorIndexService
    ) {
        this.chunkRepository = chunkRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.vectorIndexService = vectorIndexService;
    }

    public RetrievalResult search(UUID materialId, String query, int limit) {
        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore != null) {
            RetrievalResult vectorResult = searchWithVectors(materialId, query, limit, vectorStore);
            if (!vectorResult.chunks().isEmpty()) {
                return vectorResult;
            }
        }
        return searchWithKeywords(materialId, query, limit);
    }

    public List<RetrievedChunk> allChunks(UUID materialId, int limit) {
        return chunkRepository.findAllByMaterial_IdOrderByChunkIndexAsc(materialId).stream()
                .limit(limit)
                .map(chunk -> new RetrievedChunk(
                        chunk.getId(), chunk.getChunkIndex(), chunk.getPageNumber(), chunk.getContent(), 0.0
                ))
                .toList();
    }

    private RetrievalResult searchWithVectors(
            UUID materialId,
            String query,
            int limit,
            VectorStore vectorStore
    ) {
        try {
            vectorIndexService.indexIfNeeded(materialId);
            String filter = "materialId == '" + materialId + "'";
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(limit)
                    .filterExpression(filter)
                    .build());
            List<RetrievedChunk> chunks = documents.stream()
                    .map(this::toRetrievedChunk)
                    .toList();
            return new RetrievalResult("PGVECTOR", chunks);
        } catch (RuntimeException exception) {
            LOGGER.warn("Vector search failed for material {}; using keyword search instead.", materialId,
                    exception);
            return new RetrievalResult("KEYWORD_OVERLAP", List.of());
        }
    }

    private RetrievalResult searchWithKeywords(UUID materialId, String query, int limit) {
        List<String> queryTokens = tokens(query).stream().filter(token -> !STOP_WORDS.contains(token)).distinct()
                .toList();
        if (queryTokens.isEmpty()) {
            return new RetrievalResult("KEYWORD_OVERLAP", List.of());
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        List<RetrievedChunk> matches = chunkRepository.findAllByMaterial_IdOrderByChunkIndexAsc(materialId).stream()
                .map(chunk -> score(chunk, queryTokens, normalizedQuery))
                .filter(chunk -> chunk.score() > 0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed()
                        .thenComparingInt(RetrievedChunk::chunkIndex))
                .limit(limit)
                .toList();
        return new RetrievalResult("KEYWORD_OVERLAP", matches);
    }

    private RetrievedChunk score(DocumentChunk chunk, List<String> queryTokens, String normalizedQuery) {
        List<String> contentTokens = tokens(chunk.getContent());
        double score = 0;
        for (String token : queryTokens) {
            long occurrences = contentTokens.stream().filter(token::equals).count();
            if (occurrences > 0) {
                score += 1.0 + Math.min(occurrences - 1, 3) * 0.2;
            }
        }
        String normalizedContent = chunk.getContent().toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        if (!normalizedQuery.isBlank() && normalizedContent.contains(normalizedQuery)) {
            score += 2.0;
        }
        return new RetrievedChunk(
                chunk.getId(), chunk.getChunkIndex(), chunk.getPageNumber(), chunk.getContent(), score
        );
    }

    private RetrievedChunk toRetrievedChunk(Document document) {
        UUID id = UUID.fromString(document.getId());
        int pageNumber = metadataInteger(document, "pageNumber");
        int chunkIndex = metadataInteger(document, "chunkIndex");
        return new RetrievedChunk(id, chunkIndex, pageNumber, document.getText(), 0.0);
    }

    private int metadataInteger(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> tokens(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        List<String> tokens = new java.util.ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}

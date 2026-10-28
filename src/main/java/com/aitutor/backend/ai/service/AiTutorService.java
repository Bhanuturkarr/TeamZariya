package com.aitutor.backend.ai.service;

import com.aitutor.backend.ai.dto.request.AskMaterialRequest;
import com.aitutor.backend.ai.dto.request.GenerateQuestionsRequest;
import com.aitutor.backend.ai.dto.response.AiAnswerResponse;
import com.aitutor.backend.ai.dto.response.AiQuestionResponse;
import com.aitutor.backend.ai.dto.response.AiQuestionsResponse;
import com.aitutor.backend.ai.dto.response.AiTopicResponse;
import com.aitutor.backend.ai.dto.response.AiTopicsResponse;
import com.aitutor.backend.ai.dto.response.SourceReferenceResponse;
import com.aitutor.backend.ai.exception.AiOutputValidationException;
import com.aitutor.backend.ai.model.AiAnswerDraft;
import com.aitutor.backend.ai.model.AiQuestionDraft;
import com.aitutor.backend.ai.model.AiQuestionSetDraft;
import com.aitutor.backend.ai.model.AiTopicDraft;
import com.aitutor.backend.ai.model.AiTopicSetDraft;
import com.aitutor.backend.ai.model.RetrievalResult;
import com.aitutor.backend.ai.model.RetrievedChunk;
import com.aitutor.backend.document.exception.MaterialNotFoundException;
import com.aitutor.backend.repository.MaterialRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AiTutorService {

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final int MAX_CONTEXT_CHUNKS = 20;

    private final MaterialRepository materialRepository;
    private final RelevantChunkRetriever retriever;
    private final TutorAiProvider aiProvider;

    public AiTutorService(
            MaterialRepository materialRepository,
            RelevantChunkRetriever retriever,
            TutorAiProvider aiProvider
    ) {
        this.materialRepository = materialRepository;
        this.retriever = retriever;
        this.aiProvider = aiProvider;
    }

    public AiAnswerResponse answer(AskMaterialRequest request, java.util.UUID materialId) {
        requireMaterial(materialId);
        RetrievalResult retrieval = retriever.search(materialId, request.question().trim(), 4);
        AiAnswerDraft draft = aiProvider.answer(request.question().trim(), retrieval.chunks());
        validateAnswer(draft, retrieval.chunks());
        Set<Integer> citedPages = Set.copyOf(draft.citedPages());
        List<SourceReferenceResponse> sources = retrieval.chunks().stream()
                .filter(chunk -> citedPages.contains(chunk.pageNumber()))
                .map(this::toSource)
                .toList();
        return new AiAnswerResponse(
                aiProvider.providerName(),
                retrieval.mode(),
                draft.answer().trim(),
                sources,
                Instant.now()
        );
    }

    public AiQuestionsResponse generateQuestions(GenerateQuestionsRequest request, java.util.UUID materialId) {
        requireMaterial(materialId);
        RetrievalResult retrieval = retriever.search(materialId, request.topic().trim(), 10);
        AiQuestionSetDraft draft = aiProvider.generateQuestions(
                request.topic().trim(), request.count(), retrieval.chunks()
        );
        validateQuestions(draft, request, retrieval.chunks());
        List<AiQuestionResponse> questions = draft.questions().stream()
                .map(question -> new AiQuestionResponse(
                        question.question().trim(),
                        List.copyOf(question.options()),
                        question.difficulty().toUpperCase(Locale.ROOT),
                        request.topic().trim(),
                        question.sourcePage()
                ))
                .toList();
        return new AiQuestionsResponse(aiProvider.providerName(), retrieval.mode(), questions, Instant.now());
    }

    public AiQuestionSetDraft generateAssessmentQuestions(
            String topic,
            int count,
            List<RetrievedChunk> sources
    ) {
        GenerateQuestionsRequest request = new GenerateQuestionsRequest(topic, count);
        AiQuestionSetDraft draft = aiProvider.generateQuestions(topic, count, sources);
        validateQuestions(draft, request, sources);
        return draft;
    }

    public String providerName() {
        return aiProvider.providerName();
    }

    public AiTopicsResponse suggestTopics(java.util.UUID materialId) {
        requireMaterial(materialId);
        List<RetrievedChunk> chunks = retriever.allChunks(materialId, MAX_CONTEXT_CHUNKS);
        AiTopicSetDraft draft = aiProvider.suggestTopics(chunks);
        validateTopics(draft, chunks);
        List<AiTopicResponse> topics = draft.topics().stream()
                .map(topic -> new AiTopicResponse(
                        topic.name().trim(),
                        topic.description().trim(),
                        topic.sourcePage(),
                        List.copyOf(topic.prerequisites())
                ))
                .toList();
        return new AiTopicsResponse(aiProvider.providerName(), "FULL_TEXT", topics, Instant.now());
    }

    private void requireMaterial(java.util.UUID materialId) {
        if (!materialRepository.existsById(materialId)) {
            throw new MaterialNotFoundException(materialId);
        }
    }

    private void validateAnswer(AiAnswerDraft draft, List<RetrievedChunk> sources) {
        if (draft == null || draft.answer() == null || draft.answer().isBlank() || draft.answer().length() > 5000
                || draft.citedPages() == null || draft.citedPages().size() > 8) {
            throw new AiOutputValidationException("The generated answer has an invalid shape.");
        }
        Set<Integer> availablePages = sources.stream().map(RetrievedChunk::pageNumber).collect(java.util.stream.Collectors.toSet());
        if (!availablePages.containsAll(draft.citedPages())) {
            throw new AiOutputValidationException("The generated answer cites pages outside the retrieved source text.");
        }
        if (!sources.isEmpty() && draft.citedPages().isEmpty()) {
            throw new AiOutputValidationException("The generated answer did not cite its retrieved source text.");
        }
    }

    private void validateQuestions(
            AiQuestionSetDraft draft,
            GenerateQuestionsRequest request,
            List<RetrievedChunk> sources
    ) {
        if (draft == null || draft.questions() == null || draft.questions().size() > request.count()) {
            throw new AiOutputValidationException("The generated question set has an invalid shape.");
        }
        if (sources.isEmpty()) {
            if (!draft.questions().isEmpty()) {
                throw new AiOutputValidationException("Questions cannot be generated without relevant source text.");
            }
            return;
        }
        if (draft.questions().size() != request.count()) {
            throw new AiOutputValidationException("The generated question count does not match the request.");
        }

        Set<Integer> availablePages = sources.stream().map(RetrievedChunk::pageNumber).collect(java.util.stream.Collectors.toSet());
        for (AiQuestionDraft question : draft.questions()) {
            if (question == null || question.question() == null || question.question().isBlank()
                    || question.question().length() > 500 || question.options() == null
                    || question.options().size() != 4 || question.options().stream().anyMatch(option ->
                    option == null || option.isBlank() || option.length() > 500)
                    || new HashSet<>(question.options().stream()
                    .map(option -> option.trim().toLowerCase(Locale.ROOT)).toList()).size() != 4
                    || question.correctOptionIndex() < 0 || question.correctOptionIndex() >= 4
                    || question.explanation() == null || question.explanation().isBlank()
                    || question.explanation().length() > 1000
                    || question.difficulty() == null
                    || !DIFFICULTIES.contains(question.difficulty().toUpperCase(Locale.ROOT))
                    || !availablePages.contains(question.sourcePage())) {
                throw new AiOutputValidationException("A generated question is missing required grounded fields.");
            }
        }
    }

    private void validateTopics(AiTopicSetDraft draft, List<RetrievedChunk> sources) {
        if (draft == null || draft.topics() == null || draft.topics().size() > 8) {
            throw new AiOutputValidationException("The generated topic set has an invalid shape.");
        }
        if (sources.isEmpty()) {
            if (!draft.topics().isEmpty()) {
                throw new AiOutputValidationException("Topics cannot be generated without source text.");
            }
            return;
        }

        Set<Integer> availablePages = sources.stream().map(RetrievedChunk::pageNumber).collect(java.util.stream.Collectors.toSet());
        Set<String> uniqueNames = new HashSet<>();
        for (AiTopicDraft topic : draft.topics()) {
            if (topic == null || topic.name() == null || topic.name().isBlank() || topic.name().length() > 120
                    || topic.description() == null || topic.description().isBlank() || topic.description().length() > 500
                    || !availablePages.contains(topic.sourcePage()) || topic.prerequisites() == null
                    || topic.prerequisites().size() > 8 || topic.prerequisites().stream().anyMatch(prerequisite ->
                    prerequisite == null || prerequisite.isBlank() || prerequisite.length() > 120)
                    || !uniqueNames.add(topic.name().trim().toLowerCase(Locale.ROOT))) {
                throw new AiOutputValidationException("A generated topic is missing required grounded fields.");
            }
        }
    }

    private SourceReferenceResponse toSource(RetrievedChunk chunk) {
        String excerpt = chunk.content().replaceAll("\\s+", " ").trim();
        if (excerpt.length() > 500) {
            excerpt = excerpt.substring(0, 500).stripTrailing() + "…";
        }
        return new SourceReferenceResponse(chunk.id(), chunk.chunkIndex(), chunk.pageNumber(), excerpt);
    }
}

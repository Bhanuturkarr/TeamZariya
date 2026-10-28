package com.aitutor.backend.ai.service;

import com.aitutor.backend.ai.model.AiAnswerDraft;
import com.aitutor.backend.ai.model.AiQuestionDraft;
import com.aitutor.backend.ai.model.AiQuestionSetDraft;
import com.aitutor.backend.ai.model.AiTopicDraft;
import com.aitutor.backend.ai.model.AiTopicSetDraft;
import com.aitutor.backend.ai.model.RetrievedChunk;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class DeterministicMockTutorAiProvider implements TutorAiProvider {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}][\\p{L}\\p{N}-]{3,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "also", "because", "been", "between", "both", "could", "does",
            "during", "each", "from", "have", "into", "more", "most", "other", "over", "such",
            "than", "that", "their", "there", "these", "they", "this", "through", "under", "using",
            "very", "were", "what", "when", "where", "which", "while", "with", "within", "would",
            "your", "will", "then", "them", "some", "many", "only", "being", "used", "chapter",
            "section", "contents", "introduction", "overview", "summary", "references", "material"
    );

    @Override
    public String providerName() {
        return "deterministic-mock";
    }

    @Override
    public AiAnswerDraft answer(String question, List<RetrievedChunk> sources) {
        if (sources.isEmpty()) {
            return new AiAnswerDraft(
                    "The uploaded material has no passage that matches this question closely enough.",
                    List.of()
            );
        }
        String excerpts = sources.stream()
                .limit(3)
                .map(source -> "Page " + source.pageNumber() + ": " + excerpt(source.content(), 420))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
        return new AiAnswerDraft(
                "Deterministic mock response. Relevant passages from the uploaded material:\n" + excerpts,
                sources.stream().limit(3).map(RetrievedChunk::pageNumber).distinct().toList()
        );
    }

    @Override
    public AiQuestionSetDraft generateQuestions(String topic, int count, List<RetrievedChunk> sources) {
        if (sources.isEmpty()) {
            return new AiQuestionSetDraft(List.of());
        }
        List<AiQuestionDraft> questions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            RetrievedChunk source = sources.get(index % sources.size());
            String excerpt = excerpt(source.content(), 280);
            String question = switch (index) {
                case 0 -> "Which statement about " + topic + " appears in the source?";
                case 1 -> "Which detail does the source provide about " + topic + "?";
                case 2 -> "Which excerpt is relevant to " + topic + "?";
                case 3 -> "Which statement is supported by the material about " + topic + "?";
                default -> "Which source passage mentions " + topic + "?";
            };
            questions.add(new AiQuestionDraft(
                    question,
                    List.of(
                            excerpt,
                            "The selected source passage does not mention this topic.",
                            "The selected source says this topic is unrelated to the material.",
                            "The selected source provides no information about this topic."
                    ),
                    0,
                    "This mock question uses a verbatim excerpt selected from page " + source.pageNumber() + ".",
                    "EASY",
                    source.pageNumber()
            ));
        }
        return new AiQuestionSetDraft(List.copyOf(questions));
    }

    @Override
    public AiTopicSetDraft suggestTopics(List<RetrievedChunk> sources) {
        if (sources.isEmpty()) {
            return new AiTopicSetDraft(List.of());
        }
        Map<String, Integer> frequencies = new HashMap<>();
        Map<String, Integer> firstPages = new LinkedHashMap<>();
        for (RetrievedChunk source : sources) {
            Matcher matcher = TOKEN_PATTERN.matcher(source.content().toLowerCase(Locale.ROOT));
            while (matcher.find()) {
                String token = matcher.group();
                if (!STOP_WORDS.contains(token)) {
                    frequencies.merge(token, 1, Integer::sum);
                    firstPages.putIfAbsent(token, source.pageNumber());
                }
            }
        }

        List<AiTopicDraft> topics = frequencies.keySet().stream()
                .sorted(Comparator.comparingInt((String token) -> frequencies.get(token)).reversed()
                        .thenComparingInt(firstPages::get)
                        .thenComparing(Comparator.naturalOrder()))
                .limit(8)
                .map(token -> new AiTopicDraft(
                        titleCase(token),
                        "Candidate concept selected by the deterministic mock from repeated terms in the source text.",
                        firstPages.get(token),
                        List.of()
                ))
                .toList();
        if (topics.isEmpty()) {
            topics = List.of(new AiTopicDraft(
                    "General Concepts",
                    "Fallback topic candidate because no repeated topic terms were found.",
                    sources.getFirst().pageNumber(),
                    List.of()
            ));
        }
        return new AiTopicSetDraft(topics);
    }

    private String excerpt(String content, int maximumLength) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maximumLength
                ? normalized
                : normalized.substring(0, maximumLength).stripTrailing() + "…";
    }

    private String titleCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}

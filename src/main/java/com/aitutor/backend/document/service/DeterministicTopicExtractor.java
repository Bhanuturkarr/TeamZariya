package com.aitutor.backend.document.service;

import com.aitutor.backend.document.TopicExtractionMethod;
import org.springframework.stereotype.Component;

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
public class DeterministicTopicExtractor {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}][\\p{L}\\p{N}-]{3,}");
    private static final Pattern LEADING_MARKER = Pattern.compile("^[\\d.()\\-•*\\s]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "about", "after", "also", "because", "between", "both", "could", "does", "during",
            "each", "from", "have", "into", "more", "most", "other", "over", "such", "than",
            "that", "their", "there", "these", "they", "this", "through", "under", "using",
            "very", "were", "what", "when", "where", "which", "while", "with", "within",
            "would", "your", "will", "then", "them", "some", "many", "only", "being", "used",
            "chapter", "section", "contents", "introduction", "overview", "summary", "references"
    );

    public List<TopicCandidate> extract(List<PageText> pages) {
        Map<String, TopicCandidate> headings = new LinkedHashMap<>();
        for (PageText page : pages) {
            for (String rawLine : page.text().split("\\R")) {
                String heading = normalizeHeading(rawLine);
                if (heading != null) {
                    headings.putIfAbsent(key(heading),
                            new TopicCandidate(heading, page.pageNumber(), TopicExtractionMethod.HEADING));
                }
            }
        }
        if (!headings.isEmpty()) {
            return headings.values().stream().limit(25).toList();
        }

        List<TopicCandidate> keywords = extractKeywords(pages);
        if (!keywords.isEmpty()) {
            return keywords;
        }
        return List.of(new TopicCandidate("General Concepts", 1, TopicExtractionMethod.FALLBACK));
    }

    private String normalizeHeading(String rawLine) {
        String heading = LEADING_MARKER.matcher(rawLine.trim()).replaceFirst("")
                .replaceAll("\\s+", " ")
                .trim();
        if (heading.length() < 4 || heading.length() > 80 || heading.matches(".*[.!?;:]$")) {
            return null;
        }
        int wordCount = heading.split("\\s+").length;
        if (wordCount > 10 || STOP_WORDS.contains(heading.toLowerCase(Locale.ROOT))) {
            return null;
        }
        if (!heading.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} &'()/\\-]*")) {
            return null;
        }
        return heading;
    }

    private List<TopicCandidate> extractKeywords(List<PageText> pages) {
        Map<String, Integer> frequencies = new HashMap<>();
        Map<String, Integer> firstPages = new HashMap<>();
        for (PageText page : pages) {
            Matcher matcher = TOKEN_PATTERN.matcher(page.text().toLowerCase(Locale.ROOT));
            while (matcher.find()) {
                String token = matcher.group();
                if (!STOP_WORDS.contains(token)) {
                    frequencies.merge(token, 1, Integer::sum);
                    firstPages.putIfAbsent(token, page.pageNumber());
                }
            }
        }

        return frequencies.keySet().stream()
                .sorted(Comparator.comparingInt((String token) -> frequencies.get(token)).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .limit(8)
                .map(token -> new TopicCandidate(
                        titleCase(token),
                        firstPages.get(token),
                        TopicExtractionMethod.KEYWORD
                ))
                .toList();
    }

    private String titleCase(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String key(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}

package com.aitutor.backend.document.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final int maxChunkCharacters;

    public TextChunker(@Value("${app.materials.chunk-size:1500}") int maxChunkCharacters) {
        this.maxChunkCharacters = Math.max(100, maxChunkCharacters);
    }

    public List<TextChunk> chunk(List<PageText> pages) {
        List<TextChunk> chunks = new ArrayList<>();
        int chunkIndex = 1;

        for (PageText page : pages) {
            String text = normalize(page.text());
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + maxChunkCharacters, text.length());
                if (end < text.length()) {
                    int boundary = Math.max(text.lastIndexOf(' ', end), text.lastIndexOf('\n', end));
                    if (boundary > start + maxChunkCharacters / 2) {
                        end = boundary;
                    }
                }

                String content = text.substring(start, end).trim();
                if (!content.isEmpty()) {
                    chunks.add(new TextChunk(chunkIndex++, page.pageNumber(), content));
                }
                start = end;
                while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                    start++;
                }
            }
        }
        return List.copyOf(chunks);
    }

    private String normalize(String value) {
        return value.replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}

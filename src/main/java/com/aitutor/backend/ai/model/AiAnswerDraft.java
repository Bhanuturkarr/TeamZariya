package com.aitutor.backend.ai.model;

import java.util.List;

public record AiAnswerDraft(String answer, List<Integer> citedPages) {
}

package com.aitutor.backend.ai.service;

import com.aitutor.backend.ai.model.AiAnswerDraft;
import com.aitutor.backend.ai.model.AiQuestionSetDraft;
import com.aitutor.backend.ai.model.AiTopicSetDraft;
import com.aitutor.backend.ai.model.RetrievedChunk;

import java.util.List;

public interface TutorAiProvider {

    String providerName();

    AiAnswerDraft answer(String question, List<RetrievedChunk> sources);

    AiQuestionSetDraft generateQuestions(String topic, int count, List<RetrievedChunk> sources);

    AiTopicSetDraft suggestTopics(List<RetrievedChunk> sources);
}

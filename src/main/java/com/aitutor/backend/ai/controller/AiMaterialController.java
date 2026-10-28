package com.aitutor.backend.ai.controller;

import com.aitutor.backend.ai.dto.request.AskMaterialRequest;
import com.aitutor.backend.ai.dto.request.GenerateQuestionsRequest;
import com.aitutor.backend.ai.dto.response.AiAnswerResponse;
import com.aitutor.backend.ai.dto.response.AiQuestionsResponse;
import com.aitutor.backend.ai.dto.response.AiTopicsResponse;
import com.aitutor.backend.ai.service.AiTutorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequestMapping("/api/v1/materials/{materialId}/ai")
public class AiMaterialController {

    private final AiTutorService service;

    public AiMaterialController(AiTutorService service) {
        this.service = service;
    }

    @PostMapping("/answer")
    public AiAnswerResponse answer(
            @PathVariable UUID materialId,
            @Valid @RequestBody AskMaterialRequest request
    ) {
        return service.answer(request, materialId);
    }

    @PostMapping("/questions")
    public AiQuestionsResponse generateQuestions(
            @PathVariable UUID materialId,
            @Valid @RequestBody GenerateQuestionsRequest request
    ) {
        return service.generateQuestions(request, materialId);
    }

    @PostMapping("/topics")
    public AiTopicsResponse suggestTopics(@PathVariable UUID materialId) {
        return service.suggestTopics(materialId);
    }
}

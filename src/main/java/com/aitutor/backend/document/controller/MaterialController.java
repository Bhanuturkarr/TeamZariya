package com.aitutor.backend.document.controller;

import com.aitutor.backend.document.service.MaterialService;
import com.aitutor.backend.dto.response.MaterialDetailResponse;
import com.aitutor.backend.dto.response.MaterialResponse;
import com.aitutor.backend.dto.response.TopicResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequestMapping("/api/v1/materials")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @GetMapping("/{materialId}")
    public MaterialDetailResponse get(@PathVariable UUID materialId) {
        return service.get(materialId);
    }

    @GetMapping("/{materialId}/topics")
    public List<TopicResponse> topics(@PathVariable UUID materialId) {
        return service.topics(materialId);
    }

    @PostMapping("/{materialId}/reprocess")
    public MaterialResponse reprocess(@PathVariable UUID materialId) {
        return service.reprocess(materialId);
    }
}

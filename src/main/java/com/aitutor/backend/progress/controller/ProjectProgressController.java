package com.aitutor.backend.progress.controller;

import com.aitutor.backend.progress.dto.ProjectActivityEvent;
import com.aitutor.backend.progress.dto.ProjectProgressResponse;
import com.aitutor.backend.progress.dto.ProjectRecommendationResponse;
import com.aitutor.backend.progress.dto.TopicMasteryResponse;
import com.aitutor.backend.progress.service.ProjectProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequestMapping("/api/v1/projects/{projectId}")
public class ProjectProgressController {
    private final ProjectProgressService service;

    public ProjectProgressController(ProjectProgressService service) { this.service = service; }

    @GetMapping("/progress")
    public ProjectProgressResponse progress(@PathVariable UUID projectId) { return service.progress(projectId); }

    @GetMapping("/mastery")
    public List<TopicMasteryResponse> mastery(@PathVariable UUID projectId) { return service.mastery(projectId); }

    @GetMapping("/recommendations")
    public ProjectRecommendationResponse recommendations(@PathVariable UUID projectId) {
        return service.recommendation(projectId);
    }

    @GetMapping("/activity")
    public List<ProjectActivityEvent> activity(@PathVariable UUID projectId) { return service.activity(projectId); }
}

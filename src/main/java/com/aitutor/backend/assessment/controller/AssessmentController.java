package com.aitutor.backend.assessment.controller;

import com.aitutor.backend.assessment.dto.request.CreateDiagnosticAssessmentRequest;
import com.aitutor.backend.assessment.dto.request.SubmitAssessmentRequest;
import com.aitutor.backend.assessment.dto.response.AssessmentAttemptResultResponse;
import com.aitutor.backend.assessment.dto.response.AssessmentResponse;
import com.aitutor.backend.assessment.dto.response.AssessmentResultsResponse;
import com.aitutor.backend.assessment.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequestMapping("/api/v1")
public class AssessmentController {

    private final AssessmentService service;

    public AssessmentController(AssessmentService service) {
        this.service = service;
    }

    @PostMapping("/projects/{projectId}/assessments/diagnostic")
    public ResponseEntity<AssessmentResponse> createDiagnostic(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateDiagnosticAssessmentRequest request
    ) {
        AssessmentResponse created = service.createDiagnostic(projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/assessments/" + created.id())).body(created);
    }

    @GetMapping("/assessments/{assessmentId}")
    public AssessmentResponse getAssessment(@PathVariable UUID assessmentId) {
        return service.getAssessment(assessmentId);
    }

    @PostMapping("/assessments/{assessmentId}/submit")
    public AssessmentAttemptResultResponse submit(
            @PathVariable UUID assessmentId,
            @Valid @RequestBody SubmitAssessmentRequest request
    ) {
        return service.submit(assessmentId, request);
    }

    @GetMapping("/assessments/{assessmentId}/results")
    public AssessmentResultsResponse getResults(@PathVariable UUID assessmentId) {
        return service.getResults(assessmentId);
    }
}

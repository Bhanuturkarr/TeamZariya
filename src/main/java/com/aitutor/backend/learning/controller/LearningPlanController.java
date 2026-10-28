package com.aitutor.backend.learning.controller;

import com.aitutor.backend.learning.dto.request.CreateLearningPlanRequest;
import com.aitutor.backend.learning.dto.request.SubmitExerciseRequest;
import com.aitutor.backend.learning.dto.response.ExerciseSubmissionResponse;
import com.aitutor.backend.learning.dto.response.LearningExerciseResponse;
import com.aitutor.backend.learning.dto.response.LearningLessonResponse;
import com.aitutor.backend.learning.dto.response.LearningPlanLessonsResponse;
import com.aitutor.backend.learning.dto.response.LearningPlanResponse;
import com.aitutor.backend.learning.service.LearningPlanService;
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
public class LearningPlanController {

    private final LearningPlanService service;

    public LearningPlanController(LearningPlanService service) {
        this.service = service;
    }

    @PostMapping("/projects/{projectId}/learning-plans")
    public ResponseEntity<LearningPlanResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateLearningPlanRequest request
    ) {
        LearningPlanResponse created = service.create(projectId, request);
        return ResponseEntity.created(URI.create("/api/v1/learning-plans/" + created.id())).body(created);
    }

    @GetMapping("/learning-plans/{planId}")
    public LearningPlanResponse getPlan(@PathVariable UUID planId) {
        return service.getPlan(planId);
    }

    @GetMapping("/learning-plans/{planId}/lessons")
    public LearningPlanLessonsResponse getLessons(@PathVariable UUID planId) {
        return service.getLessons(planId);
    }

    @GetMapping("/lessons/{lessonId}")
    public LearningLessonResponse getLesson(@PathVariable UUID lessonId) {
        return service.getLesson(lessonId);
    }

    @PostMapping("/lessons/{lessonId}/exercises")
    public ResponseEntity<LearningExerciseResponse> createExercise(@PathVariable UUID lessonId) {
        LearningExerciseResponse exercise = service.generateExercise(lessonId);
        return ResponseEntity.created(URI.create("/api/v1/exercises/" + exercise.id())).body(exercise);
    }

    @PostMapping("/exercises/{exerciseId}/submit")
    public ExerciseSubmissionResponse submitExercise(
            @PathVariable UUID exerciseId,
            @Valid @RequestBody SubmitExerciseRequest request
    ) {
        return service.submitExercise(exerciseId, request);
    }
}

package com.aitutor.backend.security;

import com.aitutor.backend.assessment.exception.AssessmentNotFoundException;
import com.aitutor.backend.document.exception.MaterialNotFoundException;
import com.aitutor.backend.entity.Exercise;
import com.aitutor.backend.entity.LearningPlan;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Lesson;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.entity.Assessment;
import com.aitutor.backend.exception.ProjectNotFoundException;
import com.aitutor.backend.learning.exception.LearningResourceNotFoundException;
import com.aitutor.backend.repository.AssessmentRepository;
import com.aitutor.backend.repository.ExerciseRepository;
import com.aitutor.backend.repository.LearningPlanRepository;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.LessonRepository;
import com.aitutor.backend.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectOwnershipService {
    private final LearningProjectRepository projects;
    private final MaterialRepository materials;
    private final AssessmentRepository assessments;
    private final LearningPlanRepository plans;
    private final LessonRepository lessons;
    private final ExerciseRepository exercises;

    public ProjectOwnershipService(LearningProjectRepository projects, MaterialRepository materials,
                                   AssessmentRepository assessments, LearningPlanRepository plans,
                                   LessonRepository lessons, ExerciseRepository exercises) {
        this.projects = projects;
        this.materials = materials;
        this.assessments = assessments;
        this.plans = plans;
        this.lessons = lessons;
        this.exercises = exercises;
    }

    @Transactional(readOnly = true)
    public void requireProject(UUID id, String username) {
        LearningProject project = projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
        requireOwner(project, username, () -> new ProjectNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public void requireMaterial(UUID id, String username) {
        Material material = materials.findById(id).orElseThrow(() -> new MaterialNotFoundException(id));
        requireOwner(material.getProject(), username, () -> new MaterialNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public void requireAssessment(UUID id, String username) {
        Assessment assessment = assessments.findById(id).orElseThrow(() -> new AssessmentNotFoundException(id));
        requireOwner(assessment.getProject(), username, () -> new AssessmentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public void requirePlan(UUID id, String username) {
        LearningPlan plan = plans.findById(id).orElseThrow(() -> new LearningResourceNotFoundException("Learning plan " + id));
        requireOwner(plan.getProject(), username, () -> new LearningResourceNotFoundException("Learning plan " + id));
    }

    @Transactional(readOnly = true)
    public void requireLesson(UUID id, String username) {
        Lesson lesson = lessons.findById(id).orElseThrow(() -> new LearningResourceNotFoundException("Lesson " + id));
        requireOwner(lesson.getPlanItem().getPlan().getProject(), username,
                () -> new LearningResourceNotFoundException("Lesson " + id));
    }

    @Transactional(readOnly = true)
    public void requireExercise(UUID id, String username) {
        Exercise exercise = exercises.findById(id).orElseThrow(() -> new LearningResourceNotFoundException("Exercise " + id));
        requireOwner(exercise.getLesson().getPlanItem().getPlan().getProject(), username,
                () -> new LearningResourceNotFoundException("Exercise " + id));
    }

    private void requireOwner(LearningProject project, String username, java.util.function.Supplier<? extends RuntimeException> notFound) {
        if (!project.getOwner().getUsername().equals(username)) throw notFound.get();
    }
}

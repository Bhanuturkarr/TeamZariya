package com.aitutor.backend.progress.service;

import com.aitutor.backend.entity.AssessmentAttempt;
import com.aitutor.backend.entity.ExerciseAttempt;
import com.aitutor.backend.entity.LearningPlan;
import com.aitutor.backend.entity.LearningPlanItem;
import com.aitutor.backend.entity.Lesson;
import com.aitutor.backend.entity.TopicMastery;
import com.aitutor.backend.learning.exception.LearningResourceNotFoundException;
import com.aitutor.backend.progress.dto.ProjectActivityEvent;
import com.aitutor.backend.progress.dto.ProjectProgressResponse;
import com.aitutor.backend.progress.dto.ProjectRecommendationResponse;
import com.aitutor.backend.progress.dto.TopicMasteryResponse;
import com.aitutor.backend.repository.AssessmentAttemptRepository;
import com.aitutor.backend.repository.AssessmentRepository;
import com.aitutor.backend.repository.ExerciseAttemptRepository;
import com.aitutor.backend.repository.LearningPlanItemRepository;
import com.aitutor.backend.repository.LearningPlanRepository;
import com.aitutor.backend.repository.LessonRepository;
import com.aitutor.backend.repository.TopicMasteryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectProgressService {
    private final TopicMasteryRepository masteryRepository;
    private final LearningPlanRepository planRepository;
    private final LearningPlanItemRepository itemRepository;
    private final LessonRepository lessonRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final int masteryThreshold;
    private final int reviewThreshold;

    public ProjectProgressService(TopicMasteryRepository masteryRepository, LearningPlanRepository planRepository,
                                  LearningPlanItemRepository itemRepository, LessonRepository lessonRepository,
                                  AssessmentRepository assessmentRepository, AssessmentAttemptRepository assessmentAttemptRepository,
                                  ExerciseAttemptRepository exerciseAttemptRepository,
                                  @Value("${app.learning.mastery-threshold:80}") int masteryThreshold,
                                  @Value("${app.learning.review-threshold:60}") int reviewThreshold) {
        this.masteryRepository = masteryRepository;
        this.planRepository = planRepository;
        this.itemRepository = itemRepository;
        this.lessonRepository = lessonRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.masteryThreshold = masteryThreshold;
        this.reviewThreshold = reviewThreshold;
    }

    public ProjectProgressResponse progress(UUID projectId) {
        List<TopicMastery> mastery = masteryRepository.findAllByProject_Id(projectId);
        List<LearningPlan> plans = planRepository.findAllByProject_IdOrderByCreatedAtDesc(projectId);
        List<LearningPlanItem> items = plans.stream()
                .flatMap(plan -> itemRepository.findAllByPlan_IdOrderBySequenceNumberAsc(plan.getId()).stream()).toList();
        List<AssessmentAttempt> assessmentAttempts = assessmentAttemptRepository
                .findAllByAssessment_Project_IdOrderBySubmittedAtDesc(projectId);
        List<ExerciseAttempt> exerciseAttempts = exerciseAttemptRepository
                .findAllByExercise_Lesson_PlanItem_Plan_Project_IdOrderBySubmittedAtDesc(projectId);
        int average = mastery.isEmpty() ? 0 : (int) Math.round(mastery.stream().mapToInt(TopicMastery::getMasteryScore).average().orElse(0));
        Instant lastAssessment = assessmentAttempts.stream().map(AssessmentAttempt::getSubmittedAt).max(Comparator.naturalOrder()).orElse(null);
        Instant lastExercise = exerciseAttempts.stream().map(ExerciseAttempt::getSubmittedAt).max(Comparator.naturalOrder()).orElse(null);
        Instant lastActivity = lastAssessment == null ? lastExercise : lastExercise == null ? lastAssessment
                : lastAssessment.isAfter(lastExercise) ? lastAssessment : lastExercise;
        return new ProjectProgressResponse(projectId, mastery.size(), average,
                (int) mastery.stream().filter(topic -> topic.getMasteryScore() >= masteryThreshold).count(),
                (int) mastery.stream().filter(topic -> topic.getMasteryScore() < reviewThreshold).count(),
                (int) items.stream().filter(item -> item.getStatus() == LearningPlanItem.Status.COMPLETED).count(),
                items.size(), assessmentAttempts.size(), exerciseAttempts.size(),
                plans.isEmpty() ? "NOT_CREATED" : plans.getFirst().getStatus().name(), lastActivity);
    }

    public List<TopicMasteryResponse> mastery(UUID projectId) {
        return masteryRepository.findAllByProject_Id(projectId).stream()
                .sorted(Comparator.comparing(TopicMastery::getTopicName, String.CASE_INSENSITIVE_ORDER))
                .map(topic -> new TopicMasteryResponse(topic.getTopicName(), topic.getMasteryScore(),
                        topic.getDiagnosticAttempts() + topic.getPracticeAttempts(),
                        topic.getDiagnosticCorrect() + topic.getPracticeCorrect(),
                        topic.getMasteryScore() < reviewThreshold, topic.getMasteryScore() >= masteryThreshold,
                        topic.getUpdatedAt()))
                .toList();
    }

    public ProjectRecommendationResponse recommendation(UUID projectId) {
        List<LearningPlan> plans = planRepository.findAllByProject_IdOrderByCreatedAtDesc(projectId);
        List<LearningPlan> activePlans = plans.stream().filter(plan -> plan.getStatus() == LearningPlan.Status.ACTIVE).toList();
        if (activePlans.isEmpty()) {
            if (plans.isEmpty() && assessmentRepository.findAllByProject_Id(projectId).isEmpty()) {
                return new ProjectRecommendationResponse(projectId, "CREATE_DIAGNOSTIC_ASSESSMENT",
                        "Upload a processed PDF and create a diagnostic assessment to identify topics to study.",
                        null, null, null, null, null, null, null, null);
            }
            return new ProjectRecommendationResponse(projectId, "CREATE_LEARNING_PLAN",
                    "Create a learning plan from your latest assessment to get a personalized next lesson.",
                    null, null, null, null, null, null, null, null);
        }
        LearningPlan plan = activePlans.getFirst();
        LearningPlanItem item = itemRepository.findAllByPlan_IdOrderBySequenceNumberAsc(plan.getId()).stream()
                .filter(candidate -> candidate.getStatus() != LearningPlanItem.Status.COMPLETED)
                .findFirst().orElse(null);
        if (item == null) {
            return new ProjectRecommendationResponse(projectId, "REVIEW_PROGRESS", "Your current plan has no remaining topics.",
                    plan.getId(), null, null, null, null, null, null, null);
        }
        Lesson lesson = lessonRepository.findFirstByPlanItem_Id(item.getId()).orElse(null);
        TopicMastery topicMastery = masteryRepository.findByProject_IdAndTopicName(projectId, item.getTopicName()).orElse(null);
        String reason = item.getRationale();
        return new ProjectRecommendationResponse(projectId, "CONTINUE_LEARNING_PLAN", reason,
                plan.getId(), item.getSequenceNumber(), item.getTopicName(),
                topicMastery == null ? 50 : topicMastery.getMasteryScore(), item.getEstimatedMinutes(),
                lesson == null ? null : lesson.getId(), lesson == null ? null : lesson.getTitle(),
                lesson == null ? null : lesson.getSourcePage());
    }

    public List<ProjectActivityEvent> activity(UUID projectId) {
        List<ProjectActivityEvent> events = new ArrayList<>();
        for (AssessmentAttempt attempt : assessmentAttemptRepository.findAllByAssessment_Project_IdOrderBySubmittedAtDesc(projectId)) {
            events.add(new ProjectActivityEvent(attempt.getSubmittedAt(), "ASSESSMENT_ATTEMPT",
                    attempt.getAssessment().getTitle(), null, attempt.getScorePercent(), attempt.getAttemptNumber(),
                    attempt.getAssessment().getId()));
        }
        for (ExerciseAttempt attempt : exerciseAttemptRepository
                .findAllByExercise_Lesson_PlanItem_Plan_Project_IdOrderBySubmittedAtDesc(projectId)) {
            events.add(new ProjectActivityEvent(attempt.getSubmittedAt(), "EXERCISE_ATTEMPT",
                    attempt.getExercise().getLesson().getPlanItem().getTopicName(), attempt.isCorrect(), null, null,
                    attempt.getExercise().getId()));
        }
        return events.stream().sorted(Comparator.comparing(ProjectActivityEvent::occurredAt).reversed()).toList();
    }
}

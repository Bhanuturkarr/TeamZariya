package com.aitutor.backend.learning.service;

import com.aitutor.backend.ai.exception.AiOutputValidationException;
import com.aitutor.backend.ai.exception.AiProviderException;
import com.aitutor.backend.ai.dto.response.AiTopicResponse;
import com.aitutor.backend.ai.dto.response.AiTopicsResponse;
import com.aitutor.backend.ai.model.AiQuestionDraft;
import com.aitutor.backend.ai.model.AiQuestionSetDraft;
import com.aitutor.backend.ai.model.RetrievalResult;
import com.aitutor.backend.ai.model.RetrievedChunk;
import com.aitutor.backend.ai.service.AiTutorService;
import com.aitutor.backend.ai.service.RelevantChunkRetriever;
import com.aitutor.backend.assessment.exception.AssessmentNotFoundException;
import com.aitutor.backend.entity.Assessment;
import com.aitutor.backend.entity.AssessmentAnswer;
import com.aitutor.backend.entity.AssessmentAttempt;
import com.aitutor.backend.entity.AssessmentQuestion;
import com.aitutor.backend.entity.Exercise;
import com.aitutor.backend.entity.ExerciseAttempt;
import com.aitutor.backend.entity.LearningPlan;
import com.aitutor.backend.entity.LearningPlanItem;
import com.aitutor.backend.entity.Lesson;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Topic;
import com.aitutor.backend.entity.TopicMastery;
import com.aitutor.backend.entity.TopicPrerequisite;
import com.aitutor.backend.learning.dto.request.CreateLearningPlanRequest;
import com.aitutor.backend.learning.dto.request.SubmitExerciseRequest;
import com.aitutor.backend.learning.dto.response.ExerciseSubmissionResponse;
import com.aitutor.backend.learning.dto.response.LearningExerciseResponse;
import com.aitutor.backend.learning.dto.response.LearningLessonResponse;
import com.aitutor.backend.learning.dto.response.LearningPlanItemResponse;
import com.aitutor.backend.learning.dto.response.LearningPlanLessonsResponse;
import com.aitutor.backend.learning.dto.response.LearningPlanResponse;
import com.aitutor.backend.learning.exception.InvalidLearningRequestException;
import com.aitutor.backend.learning.exception.LearningResourceNotFoundException;
import com.aitutor.backend.repository.AssessmentAnswerRepository;
import com.aitutor.backend.repository.AssessmentAttemptRepository;
import com.aitutor.backend.repository.AssessmentQuestionRepository;
import com.aitutor.backend.repository.AssessmentRepository;
import com.aitutor.backend.repository.ExerciseAttemptRepository;
import com.aitutor.backend.repository.ExerciseRepository;
import com.aitutor.backend.repository.LearningPlanItemRepository;
import com.aitutor.backend.repository.LearningPlanRepository;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.LessonRepository;
import com.aitutor.backend.repository.TopicMasteryRepository;
import com.aitutor.backend.repository.TopicPrerequisiteRepository;
import com.aitutor.backend.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LearningPlanService {

    private static final int MAX_SOURCE_CHUNKS = 20;

    private final LearningProjectRepository projectRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final TopicRepository topicRepository;
    private final TopicPrerequisiteRepository prerequisiteRepository;
    private final TopicMasteryRepository masteryRepository;
    private final LearningPlanRepository planRepository;
    private final LearningPlanItemRepository planItemRepository;
    private final LessonRepository lessonRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseAttemptRepository exerciseAttemptRepository;
    private final RelevantChunkRetriever retriever;
    private final AiTutorService aiTutorService;
    private final int masteryThreshold;
    private final int reviewThreshold;

    public LearningPlanService(
            LearningProjectRepository projectRepository,
            AssessmentRepository assessmentRepository,
            AssessmentAttemptRepository assessmentAttemptRepository,
            AssessmentQuestionRepository assessmentQuestionRepository,
            AssessmentAnswerRepository assessmentAnswerRepository,
            TopicRepository topicRepository,
            TopicPrerequisiteRepository prerequisiteRepository,
            TopicMasteryRepository masteryRepository,
            LearningPlanRepository planRepository,
            LearningPlanItemRepository planItemRepository,
            LessonRepository lessonRepository,
            ExerciseRepository exerciseRepository,
            ExerciseAttemptRepository exerciseAttemptRepository,
            RelevantChunkRetriever retriever,
            AiTutorService aiTutorService,
            @Value("${app.learning.mastery-threshold:80}") int masteryThreshold,
            @Value("${app.learning.review-threshold:60}") int reviewThreshold
    ) {
        this.projectRepository = projectRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentAttemptRepository = assessmentAttemptRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.assessmentAnswerRepository = assessmentAnswerRepository;
        this.topicRepository = topicRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.masteryRepository = masteryRepository;
        this.planRepository = planRepository;
        this.planItemRepository = planItemRepository;
        this.lessonRepository = lessonRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseAttemptRepository = exerciseAttemptRepository;
        this.retriever = retriever;
        this.aiTutorService = aiTutorService;
        this.masteryThreshold = masteryThreshold;
        this.reviewThreshold = reviewThreshold;
        if (masteryThreshold < 1 || masteryThreshold > 100
                || reviewThreshold < 0 || reviewThreshold >= masteryThreshold) {
            throw new IllegalArgumentException("Learning thresholds must satisfy 0 <= review < mastery <= 100.");
        }
    }

    @Transactional
    public LearningPlanResponse create(UUID projectId, CreateLearningPlanRequest request) {
        LearningProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new LearningResourceNotFoundException("Project " + projectId));
        Assessment assessment = assessmentRepository.findById(request.assessmentId())
                .orElseThrow(() -> new AssessmentNotFoundException(request.assessmentId()));
        if (!assessment.getProject().getId().equals(projectId)) {
            throw new InvalidLearningRequestException("The assessment does not belong to this project.");
        }
        AssessmentAttempt attempt = assessmentAttemptRepository
                .findFirstByAssessment_IdOrderByAttemptNumberDesc(assessment.getId())
                .orElseThrow(() -> new InvalidLearningRequestException(
                        "Submit the diagnostic assessment before creating a learning plan."));
        List<AssessmentQuestion> diagnosticQuestions = assessmentQuestionRepository
                .findAllByAssessment_IdOrderByQuestionOrderAsc(assessment.getId());
        List<AssessmentAnswer> diagnosticAnswers = assessmentAnswerRepository
                .findAllByAttempt_IdOrderByQuestion_QuestionOrderAsc(attempt.getId());
        Map<String, int[]> diagnosticScores = diagnosticScores(diagnosticQuestions, diagnosticAnswers);

        List<Topic> topics = topicRepository.findAllByMaterial_IdOrderBySourcePageAscNameAsc(
                assessment.getMaterial().getId());
        if (topics.isEmpty()) {
            throw new InvalidLearningRequestException("The assessment material has no extracted topics to plan from.");
        }

        Map<String, TopicMastery> masteryByTopic = masteryForProject(projectId);
        for (Topic topic : topics) {
            String name = topic.getName();
            int[] score = diagnosticScores.getOrDefault(name, new int[]{0, 0});
            TopicMastery mastery = masteryByTopic.getOrDefault(name, new TopicMastery(project, topic, name));
            if (score[1] > 0) {
                mastery.setDiagnostic(attempt, score[0], score[1]);
            }
            masteryByTopic.put(name, masteryRepository.save(mastery));
        }

        List<TopicPrerequisite> prerequisites = ensurePrerequisites(assessment.getMaterial().getId(), topics);
        Map<UUID, Set<UUID>> prerequisiteIds = prerequisiteMap(prerequisites, topics);
        List<Topic> orderedTopics = topologicalOrder(topics, prerequisiteIds, masteryByTopic);

        LearningPlan plan = planRepository.save(new LearningPlan(
                project,
                assessment.getMaterial(),
                assessment,
                attempt,
                request.effectiveStudyMinutes(),
                normalizePreference(request.learningPreference())
        ));
        List<LearningPlanItem> items = new ArrayList<>();
        int topicCount = Math.max(1, orderedTopics.size());
        int baseMinutes = request.effectiveStudyMinutes() / topicCount;
        int extraMinutes = request.effectiveStudyMinutes() % topicCount;
        for (int index = 0; index < orderedTopics.size(); index++) {
            Topic topic = orderedTopics.get(index);
            TopicMastery mastery = masteryByTopic.get(topic.getName());
            List<String> prerequisiteNames = prerequisiteIds.getOrDefault(topic.getId(), Set.of()).stream()
                    .map(id -> topics.stream().filter(candidate -> candidate.getId().equals(id))
                            .map(Topic::getName).findFirst().orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .sorted()
                    .toList();
            LearningPlanItem item = planItemRepository.save(new LearningPlanItem(
                    plan,
                    topic,
                    topic.getName(),
                    index + 1,
                    Math.max(1, baseMinutes + (index < extraMinutes ? 1 : 0)),
                    rationale(mastery.getMasteryScore(), prerequisiteNames)
            ));
            if (index == 0) item.start();
            createLesson(item, topic, mastery.getMasteryScore(), assessment.getMaterial().getId(),
                    plan.getLearningPreference());
            items.add(item);
        }
        return toPlanResponse(plan, items, masteryByTopic, prerequisites, topics);
    }

    @Transactional(readOnly = true)
    public LearningPlanResponse getPlan(UUID planId) {
        LearningPlan plan = requirePlan(planId);
        return toPlanResponse(plan,
                planItemRepository.findAllByPlan_IdOrderBySequenceNumberAsc(planId),
                masteryForProject(plan.getProject().getId()),
                prerequisiteRepository.findAllByTopic_Material_Id(plan.getMaterial().getId()),
                topicRepository.findAllByMaterial_IdOrderBySourcePageAscNameAsc(plan.getMaterial().getId()));
    }

    @Transactional(readOnly = true)
    public LearningPlanLessonsResponse getLessons(UUID planId) {
        requirePlan(planId);
        List<Lesson> lessonEntities = lessonRepository.findAllByPlanItem_Plan_IdOrderByPlanItem_SequenceNumberAsc(planId);
        Map<UUID, List<Exercise>> exercises = exercisesForLessons(lessonEntities);
        List<LearningLessonResponse> lessons = lessonEntities.stream()
                .map(lesson -> toLessonResponse(lesson, exercises.getOrDefault(lesson.getId(), List.of())))
                .toList();
        return new LearningPlanLessonsResponse(planId, lessons);
    }

    @Transactional(readOnly = true)
    public LearningLessonResponse getLesson(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .map(this::toLessonResponse)
                .orElseThrow(() -> new LearningResourceNotFoundException("Lesson " + lessonId));
    }

    @Transactional
    public LearningExerciseResponse generateExercise(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LearningResourceNotFoundException("Lesson " + lessonId));
        Exercise exercise = createExercise(
                lesson,
                lesson.getPlanItem().getTopicName(),
                lesson.getPlanItem().getPlan().getMaterial().getId()
        );
        return toExerciseResponse(exercise);
    }

    @Transactional
    public ExerciseSubmissionResponse submitExercise(UUID exerciseId, SubmitExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new LearningResourceNotFoundException("Exercise " + exerciseId));
        Lesson lesson = exercise.getLesson();
        LearningPlanItem item = lesson.getPlanItem();
        LearningPlan plan = item.getPlan();
        UUID projectId = plan.getProject().getId();
        TopicMastery mastery = masteryRepository.findForUpdate(projectId, item.getTopicName())
                .orElseGet(() -> masteryRepository.save(new TopicMastery(
                        plan.getProject(), item.getTopic(), item.getTopicName()
                )));
        int before = mastery.getMasteryScore();
        boolean correct = exercise.getCorrectOptionIndex() == request.selectedOptionIndex();
        mastery.recordPractice(correct);
        int after = mastery.getMasteryScore();
        masteryRepository.save(mastery);
        ExerciseAttempt attempt = exerciseAttemptRepository.save(new ExerciseAttempt(
                exercise, request.selectedOptionIndex(), correct, before, after
        ));

        String nextAction;
        String recommendation;
        LearningPlanItem recommendedItem = item;
        if (!correct || after < reviewThreshold) {
            plan.activate();
            item.resume();
            nextAction = "REVISE";
            recommendation = "Review the lesson and source passage on page " + lesson.getSourcePage()
                    + ", then retry the exercise.";
        } else if (after < masteryThreshold) {
            plan.activate();
            item.resume();
            nextAction = "PRACTICE";
            recommendation = "Try another practice question for " + item.getTopicName()
                    + " before moving to the next topic.";
        } else {
            item.complete();
            lesson.complete();
            List<LearningPlanItem> planItems = planItemRepository
                    .findAllByPlan_IdOrderBySequenceNumberAsc(plan.getId());
            recommendedItem = planItems.stream()
                    .filter(candidate -> candidate.getStatus() != LearningPlanItem.Status.COMPLETED)
                    .findFirst()
                    .orElse(null);
            if (recommendedItem == null) {
                plan.complete();
                nextAction = "PLAN_COMPLETE";
                recommendation = "You reached the mastery threshold for every topic in this plan.";
            } else {
                recommendedItem.start();
                nextAction = "ADVANCE";
                recommendation = "Move on to " + recommendedItem.getTopicName() + ".";
            }
        }
        String nextTopic = recommendedItem == null ? null : recommendedItem.getTopicName();
        UUID nextLessonId = recommendedItem == null ? null : lessonRepository
                .findFirstByPlanItem_Id(recommendedItem.getId()).map(Lesson::getId).orElse(null);
        return new ExerciseSubmissionResponse(
                attempt.getId(),
                correct,
                exercise.getCorrectOptionIndex(),
                exercise.getOption(exercise.getCorrectOptionIndex()),
                exercise.getExplanation(),
                before,
                after,
                nextAction,
                recommendation,
                nextTopic,
                nextLessonId,
                attempt.getSubmittedAt()
        );
    }

    private Map<String, int[]> diagnosticScores(
            List<AssessmentQuestion> questions,
            List<AssessmentAnswer> answers
    ) {
        Map<UUID, AssessmentAnswer> byQuestion = new HashMap<>();
        answers.forEach(answer -> byQuestion.put(answer.getQuestion().getId(), answer));
        Map<String, int[]> scores = new LinkedHashMap<>();
        for (AssessmentQuestion question : questions) {
            AssessmentAnswer answer = byQuestion.get(question.getId());
            if (answer == null) continue;
            int[] count = scores.computeIfAbsent(question.getTopicName(), ignored -> new int[2]);
            count[1]++;
            if (answer.isCorrect()) count[0]++;
        }
        return scores;
    }

    private List<TopicPrerequisite> ensurePrerequisites(UUID materialId, List<Topic> topics) {
        List<TopicPrerequisite> existing = prerequisiteRepository.findAllByTopic_Material_Id(materialId);
        if (!existing.isEmpty()) return existing;
        List<TopicPrerequisite> aiExtracted = extractAiPrerequisites(materialId, topics);
        if (!aiExtracted.isEmpty()) return prerequisiteRepository.saveAll(aiExtracted);
        List<TopicPrerequisite> inferred = new ArrayList<>();
        List<Topic> ordered = topics.stream().sorted(Comparator.comparingInt(Topic::getSourcePage)
                .thenComparing(Topic::getName)).toList();
        Set<String> seenPage = new HashSet<>();
        for (Topic topic : ordered) {
            if (!seenPage.add(String.valueOf(topic.getSourcePage()))) continue;
            Topic previousPageTopic = ordered.stream()
                    .filter(candidate -> candidate.getSourcePage() < topic.getSourcePage())
                    .max(Comparator.comparingInt(Topic::getSourcePage)
                            .thenComparing(Topic::getName, Comparator.reverseOrder()))
                    .orElse(null);
            if (previousPageTopic != null) {
                inferred.add(new TopicPrerequisite(topic, previousPageTopic, "SOURCE_ORDER_FALLBACK"));
            }
        }
        return prerequisiteRepository.saveAll(inferred);
    }

    private List<TopicPrerequisite> extractAiPrerequisites(UUID materialId, List<Topic> topics) {
        List<TopicPrerequisite> result = new ArrayList<>();
        try {
            AiTopicsResponse suggestions = aiTutorService.suggestTopics(materialId);
            if (suggestions == null || suggestions.topics() == null) return result;
            for (AiTopicResponse suggestion : suggestions.topics()) {
                Topic topic = matchTopic(suggestion.name(), topics);
                if (topic == null || suggestion.prerequisites() == null) continue;
                for (String prerequisiteName : suggestion.prerequisites()) {
                    Topic prerequisite = matchTopic(prerequisiteName, topics);
                    if (prerequisite != null && !prerequisite.getId().equals(topic.getId())
                            && result.stream().noneMatch(edge -> edge.getTopic().getId().equals(topic.getId())
                            && edge.getPrerequisiteTopic().getId().equals(prerequisite.getId()))) {
                        result.add(new TopicPrerequisite(topic, prerequisite, "AI_EXTRACTED"));
                    }
                }
            }
        } catch (AiProviderException | AiOutputValidationException ignored) {
            // Source order remains a deterministic prerequisite fallback if topic metadata is unavailable.
        }
        return result;
    }

    private Topic matchTopic(String candidateName, List<Topic> topics) {
        if (candidateName == null || candidateName.isBlank()) return null;
        String candidate = normalizeTopic(candidateName);
        return topics.stream()
                .map(topic -> Map.entry(topic, topicMatchScore(candidate, normalizeTopic(topic.getName()))))
                .filter(entry -> entry.getValue() >= 0.5)
                .max(Comparator.comparingDouble((Map.Entry<Topic, Double> entry) -> entry.getValue())
                        .thenComparingInt(entry -> -entry.getKey().getSourcePage()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String normalizeTopic(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
    }

    private double topicMatchScore(String first, String second) {
        if (first.equals(second)) return 1.0;
        Set<String> firstTokens = new HashSet<>(List.of(first.split("\\s+")));
        Set<String> secondTokens = new HashSet<>(List.of(second.split("\\s+")));
        long shared = firstTokens.stream().filter(secondTokens::contains).count();
        return shared / (double) Math.max(firstTokens.size(), secondTokens.size());
    }

    private Map<UUID, Set<UUID>> prerequisiteMap(List<TopicPrerequisite> prerequisites, List<Topic> topics) {
        Set<UUID> included = topics.stream().map(Topic::getId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, Set<UUID>> result = new LinkedHashMap<>();
        for (TopicPrerequisite prerequisite : prerequisites) {
            UUID topicId = prerequisite.getTopic().getId();
            UUID prerequisiteId = prerequisite.getPrerequisiteTopic().getId();
            if (included.contains(topicId) && included.contains(prerequisiteId)) {
                result.computeIfAbsent(topicId, ignored -> new LinkedHashSet<>()).add(prerequisiteId);
            }
        }
        return result;
    }

    private List<Topic> topologicalOrder(
            List<Topic> topics,
            Map<UUID, Set<UUID>> prerequisites,
            Map<String, TopicMastery> mastery
    ) {
        Comparator<Topic> priority = Comparator
                .comparingInt((Topic topic) -> mastery.get(topic.getName()).getMasteryScore())
                .thenComparingInt(Topic::getSourcePage)
                .thenComparing(Topic::getName);
        Set<UUID> remaining = topics.stream().map(Topic::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<Topic> result = new ArrayList<>();
        Map<UUID, Topic> byId = new HashMap<>();
        topics.forEach(topic -> byId.put(topic.getId(), topic));
        while (!remaining.isEmpty()) {
            Topic next = remaining.stream()
                    .map(byId::get)
                    .filter(topic -> prerequisites.getOrDefault(topic.getId(), Set.of()).stream()
                            .noneMatch(remaining::contains))
                    .min(priority)
                    .orElseGet(() -> remaining.stream().map(byId::get).min(priority).orElseThrow());
            result.add(next);
            remaining.remove(next.getId());
        }
        return result;
    }

    private Lesson createLesson(
            LearningPlanItem item,
            Topic topic,
            int masteryScore,
            UUID materialId,
            String learningPreference
    ) {
        List<RetrievedChunk> sources = sourceChunks(materialId, topic.getName());
        if (sources.isEmpty()) {
            throw new AiOutputValidationException("No source passage was found for topic " + topic.getName() + ".");
        }
        boolean concise = learningPreference != null
                && (learningPreference.toLowerCase(Locale.ROOT).contains("brief")
                || learningPreference.toLowerCase(Locale.ROOT).contains("concise"));
        String excerpt = excerpt(sources.getFirst().content(), concise ? 600 : 1200);
        int sourcePage = sources.getFirst().pageNumber();
        AiQuestionSetDraft generated = aiTutorService.generateAssessmentQuestions(topic.getName(), 1, sources);
        AiQuestionDraft exercise = generated.questions().getFirst();
        String explanation = "Key idea for " + topic.getName() + ": " + exercise.explanation().trim()
                + "\n\nSource passage from page " + sourcePage + ":\n" + excerpt;
        if (masteryScore < reviewThreshold) {
            explanation = "Focus this lesson on a diagnostic gap. " + explanation;
        }
        String objective = "Explain " + topic.getName()
                + " using the uploaded material, then answer a grounded practice question.";
        String workedExample = "Worked example from page " + sourcePage + ":\n" + excerpt
                + "\n\nUse this passage to describe the main idea of " + topic.getName() + ".";
        if (learningPreference != null && learningPreference.toLowerCase(Locale.ROOT).contains("step")) {
            workedExample = "Step 1: Read the passage from page " + sourcePage + ".\n"
                    + "Step 2: Identify its main claim.\nStep 3: Explain how it relates to " + topic.getName()
                    + ".\n\nPassage:\n" + excerpt;
        }
        Lesson lesson = lessonRepository.save(new Lesson(
                item,
                lessonTitle(topic.getName()),
                objective,
                explanation,
                workedExample,
                sourcePage
        ));
        exerciseRepository.save(toExercise(lesson, exercise));
        return lesson;
    }

    private Exercise createExercise(Lesson lesson, String topicName, UUID materialId) {
        List<RetrievedChunk> sources = sourceChunks(materialId, topicName);
        if (sources.isEmpty()) {
            throw new AiOutputValidationException("No source passage was found for topic " + topicName + ".");
        }
        AiQuestionDraft draft = aiTutorService.generateAssessmentQuestions(topicName, 1, sources)
                .questions().getFirst();
        return exerciseRepository.save(toExercise(lesson, draft));
    }

    private Exercise toExercise(Lesson lesson, AiQuestionDraft draft) {
        return new Exercise(
                lesson,
                draft.question().trim(),
                draft.options().stream().map(String::trim).toList(),
                draft.correctOptionIndex(),
                draft.explanation().trim(),
                draft.difficulty().toUpperCase(Locale.ROOT),
                draft.sourcePage()
        );
    }

    private List<RetrievedChunk> sourceChunks(UUID materialId, String topicName) {
        RetrievalResult retrieval = retriever.search(materialId, topicName, 10);
        return retrieval.chunks().isEmpty()
                ? retriever.allChunks(materialId, MAX_SOURCE_CHUNKS)
                : retrieval.chunks();
    }

    private LearningPlan requirePlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new LearningResourceNotFoundException("Learning plan " + planId));
    }

    private Map<String, TopicMastery> masteryForProject(UUID projectId) {
        Map<String, TopicMastery> result = new HashMap<>();
        masteryRepository.findAllByProject_Id(projectId)
                .forEach(mastery -> result.put(mastery.getTopicName(), mastery));
        return result;
    }

    private LearningPlanResponse toPlanResponse(
            LearningPlan plan,
            List<LearningPlanItem> items,
            Map<String, TopicMastery> masteryByTopic,
            List<TopicPrerequisite> prerequisites,
            List<Topic> topics
    ) {
        Map<UUID, Topic> topicsById = new HashMap<>();
        topics.forEach(topic -> topicsById.put(topic.getId(), topic));
        Map<UUID, List<TopicPrerequisite>> prereqsByTopic = new HashMap<>();
        prerequisites.forEach(prerequisite -> prereqsByTopic
                .computeIfAbsent(prerequisite.getTopic().getId(), ignored -> new ArrayList<>()).add(prerequisite));
        List<Lesson> lessonEntities = lessonRepository.findAllByPlanItem_IdInOrderByPlanItem_SequenceNumberAsc(
                items.stream().map(LearningPlanItem::getId).toList());
        Map<UUID, Lesson> lessonsByItem = lessonEntities.stream().collect(Collectors.toMap(
                lesson -> lesson.getPlanItem().getId(), lesson -> lesson
        ));
        Map<UUID, List<Exercise>> exercisesByLesson = exercisesForLessons(lessonEntities);
        List<LearningPlanItemResponse> itemResponses = items.stream().map(item -> {
            TopicMastery mastery = masteryByTopic.get(item.getTopicName());
            List<TopicPrerequisite> topicPrerequisites = item.getTopic() == null
                    ? List.of()
                    : prereqsByTopic.getOrDefault(item.getTopic().getId(), List.of());
            List<String> names = topicPrerequisites.stream()
                    .map(prerequisite -> topicsById.get(prerequisite.getPrerequisiteTopic().getId()))
                    .filter(java.util.Objects::nonNull)
                    .map(Topic::getName)
                    .sorted()
                    .toList();
            String method = topicPrerequisites.stream().anyMatch(edge -> edge.getSource().equals("AI_EXTRACTED"))
                    ? "AI_EXTRACTED"
                    : topicPrerequisites.stream().findFirst().map(TopicPrerequisite::getSource).orElse("NONE");
            Lesson lessonEntity = lessonsByItem.get(item.getId());
            LearningLessonResponse lesson = lessonEntity == null ? null : toLessonResponse(
                    lessonEntity, exercisesByLesson.getOrDefault(lessonEntity.getId(), List.of()));
            return new LearningPlanItemResponse(
                    item.getId(),
                    item.getSequenceNumber(),
                    item.getTopicName(),
                    mastery == null ? 50 : mastery.getMasteryScore(),
                    item.getEstimatedMinutes(),
                    item.getRationale(),
                    item.getStatus().name(),
                    method,
                    names,
                    lesson
            );
        }).toList();
        return new LearningPlanResponse(
                plan.getId(),
                plan.getProject().getId(),
                plan.getMaterial().getId(),
                plan.getAssessment().getId(),
                plan.getAssessmentAttempt().getId(),
                plan.getStatus().name(),
                plan.getAvailableStudyMinutes(),
                plan.getLearningPreference(),
                masteryThreshold,
                reviewThreshold,
                plan.getCreatedAt(),
                itemResponses
        );
    }

    private LearningLessonResponse toLessonResponse(Lesson lesson) {
        List<Exercise> exercises = exerciseRepository.findAllByLesson_IdOrderByCreatedAtAsc(lesson.getId());
        return toLessonResponse(lesson, exercises);
    }

    private LearningLessonResponse toLessonResponse(Lesson lesson, List<Exercise> exercises) {
        return new LearningLessonResponse(
                lesson.getId(),
                lesson.getPlanItem().getTopicName(),
                lesson.getTitle(),
                lesson.getObjective(),
                lesson.getExplanation(),
                lesson.getWorkedExample(),
                lesson.getSourcePage(),
                lesson.isCompleted(),
                exercises.stream().map(this::toExerciseResponse).toList()
        );
    }

    private Map<UUID, List<Exercise>> exercisesForLessons(List<Lesson> lessons) {
        if (lessons.isEmpty()) return Map.of();
        return exerciseRepository.findAllByLesson_IdInOrderByCreatedAtAsc(
                        lessons.stream().map(Lesson::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(exercise -> exercise.getLesson().getId()));
    }

    private LearningExerciseResponse toExerciseResponse(Exercise exercise) {
        return new LearningExerciseResponse(
                exercise.getId(),
                exercise.getQuestionText(),
                exercise.getOptions(),
                exercise.getDifficulty(),
                exercise.getSourcePage()
        );
    }

    private String rationale(int masteryScore, List<String> prerequisites) {
        String diagnostic = masteryScore < reviewThreshold
                ? "Diagnostic mastery is " + masteryScore + "%; revisit this concept."
                : masteryScore < masteryThreshold
                ? "Diagnostic mastery is " + masteryScore + "%; reinforce this concept with practice."
                : "Diagnostic mastery is " + masteryScore + "%; use a short review to retain it.";
        if (!prerequisites.isEmpty()) {
            diagnostic += " Complete prerequisite topic(s) first: " + String.join(", ", prerequisites) + ".";
        }
        return diagnostic;
    }

    private String lessonTitle(String topicName) {
        String title = "Learn: " + topicName;
        return title.length() <= 200 ? title : title.substring(0, 200);
    }

    private String excerpt(String content, int maximumLength) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maximumLength
                ? normalized : normalized.substring(0, maximumLength).stripTrailing() + "…";
    }

    private String normalizePreference(String preference) {
        return preference == null || preference.isBlank() ? null : preference.trim();
    }
}

package com.aitutor.backend.assessment.service;

import com.aitutor.backend.ai.exception.AiOutputValidationException;
import com.aitutor.backend.ai.model.AiQuestionDraft;
import com.aitutor.backend.ai.model.AiQuestionSetDraft;
import com.aitutor.backend.ai.model.RetrievalResult;
import com.aitutor.backend.ai.model.RetrievedChunk;
import com.aitutor.backend.ai.service.AiTutorService;
import com.aitutor.backend.ai.service.RelevantChunkRetriever;
import com.aitutor.backend.assessment.dto.request.CreateDiagnosticAssessmentRequest;
import com.aitutor.backend.assessment.dto.request.SubmitAssessmentRequest;
import com.aitutor.backend.assessment.dto.response.AssessmentAttemptResultResponse;
import com.aitutor.backend.assessment.dto.response.AssessmentQuestionFeedbackResponse;
import com.aitutor.backend.assessment.dto.response.AssessmentQuestionResponse;
import com.aitutor.backend.assessment.dto.response.AssessmentResponse;
import com.aitutor.backend.assessment.dto.response.AssessmentResultsResponse;
import com.aitutor.backend.assessment.dto.response.TopicAssessmentScoreResponse;
import com.aitutor.backend.assessment.exception.AssessmentNotFoundException;
import com.aitutor.backend.assessment.exception.InvalidAssessmentRequestException;
import com.aitutor.backend.document.MaterialStatus;
import com.aitutor.backend.document.exception.MaterialNotFoundException;
import com.aitutor.backend.entity.Assessment;
import com.aitutor.backend.entity.AssessmentAnswer;
import com.aitutor.backend.entity.AssessmentAttempt;
import com.aitutor.backend.entity.AssessmentQuestion;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.entity.Topic;
import com.aitutor.backend.exception.ProjectNotFoundException;
import com.aitutor.backend.repository.AssessmentAnswerRepository;
import com.aitutor.backend.repository.AssessmentAttemptRepository;
import com.aitutor.backend.repository.AssessmentQuestionRepository;
import com.aitutor.backend.repository.AssessmentRepository;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.MaterialRepository;
import com.aitutor.backend.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AssessmentService {

    private static final int MAX_RETRIEVED_CHUNKS = 20;

    private final LearningProjectRepository projectRepository;
    private final MaterialRepository materialRepository;
    private final TopicRepository topicRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final RelevantChunkRetriever retriever;
    private final AiTutorService aiTutorService;

    public AssessmentService(
            LearningProjectRepository projectRepository,
            MaterialRepository materialRepository,
            TopicRepository topicRepository,
            AssessmentRepository assessmentRepository,
            AssessmentQuestionRepository questionRepository,
            AssessmentAttemptRepository attemptRepository,
            AssessmentAnswerRepository answerRepository,
            RelevantChunkRetriever retriever,
            AiTutorService aiTutorService
    ) {
        this.projectRepository = projectRepository;
        this.materialRepository = materialRepository;
        this.topicRepository = topicRepository;
        this.assessmentRepository = assessmentRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.answerRepository = answerRepository;
        this.retriever = retriever;
        this.aiTutorService = aiTutorService;
    }

    @Transactional
    public AssessmentResponse createDiagnostic(
            UUID projectId,
            CreateDiagnosticAssessmentRequest request
    ) {
        LearningProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new MaterialNotFoundException(request.materialId()));
        if (!material.getProject().getId().equals(projectId)) {
            throw new InvalidAssessmentRequestException("The selected material does not belong to this project.");
        }
        if (material.getStatus() != MaterialStatus.PROCESSED) {
            throw new InvalidAssessmentRequestException("The selected PDF must be processed before an assessment can be created.");
        }

        List<Topic> topics = topicRepository.findAllByMaterial_IdOrderByNameAsc(material.getId());
        int questionCount = request.effectiveQuestionCount();
        String title = assessmentTitle(material.getOriginalFilename());
        Assessment assessment = assessmentRepository.save(new Assessment(
                project, material, title, aiTutorService.providerName()
        ));

        List<AssessmentQuestion> questions = new ArrayList<>();
        int topicCount = Math.max(1, topics.size());
        int[] questionsPerTopic = new int[topicCount];
        for (int index = 0; index < questionCount; index++) {
            questionsPerTopic[index % topicCount]++;
        }
        int questionOrder = 1;
        for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
            Topic topic = topics.isEmpty() ? null : topics.get(topicIndex);
            String topicName = topic == null ? "General Concepts" : topic.getName();
            RetrievalResult retrieval = retriever.search(material.getId(), topicName, 10);
            List<RetrievedChunk> sources = retrieval.chunks().isEmpty()
                    ? retriever.allChunks(material.getId(), MAX_RETRIEVED_CHUNKS)
                    : retrieval.chunks();
            if (sources.isEmpty()) {
                throw new AiOutputValidationException("The processed PDF has no source passages for assessment questions.");
            }
            AiQuestionSetDraft draft = aiTutorService.generateAssessmentQuestions(
                    topicName, questionsPerTopic[topicIndex], sources
            );
            for (AiQuestionDraft generated : draft.questions()) {
                questions.add(new AssessmentQuestion(
                        assessment,
                        topic,
                        topicName,
                        questionOrder++,
                        generated.question().trim(),
                        generated.options().stream().map(String::trim).toList(),
                        generated.correctOptionIndex(),
                        generated.explanation().trim(),
                        generated.difficulty().toUpperCase(),
                        generated.sourcePage()
                ));
            }
        }
        questionRepository.saveAll(questions);
        return toAssessmentResponse(assessment, questions);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getAssessment(UUID assessmentId) {
        Assessment assessment = requireAssessment(assessmentId);
        return toAssessmentResponse(assessment,
                questionRepository.findAllByAssessment_IdOrderByQuestionOrderAsc(assessmentId));
    }

    @Transactional
    public AssessmentAttemptResultResponse submit(
            UUID assessmentId,
            SubmitAssessmentRequest request
    ) {
        Assessment assessment = assessmentRepository.findByIdForUpdate(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
        List<AssessmentQuestion> questions = questionRepository.findAllByAssessment_IdOrderByQuestionOrderAsc(assessmentId);
        Map<UUID, AssessmentQuestion> questionsById = new LinkedHashMap<>();
        questions.forEach(question -> questionsById.put(question.getId(), question));
        if (questions.isEmpty() || request.answers().size() != questions.size()) {
            throw new InvalidAssessmentRequestException("Submit exactly one answer for every assessment question.");
        }

        Map<UUID, Integer> selectedOptions = new LinkedHashMap<>();
        for (SubmitAssessmentRequest.SubmittedAnswerRequest submitted : request.answers()) {
            if (submitted == null || submitted.questionId() == null || submitted.selectedOptionIndex() == null) {
                throw new InvalidAssessmentRequestException("Every submitted answer needs a question ID and option index.");
            }
            if (!questionsById.containsKey(submitted.questionId())) {
                throw new InvalidAssessmentRequestException("An answer references a question outside this assessment.");
            }
            if (selectedOptions.putIfAbsent(submitted.questionId(), submitted.selectedOptionIndex()) != null) {
                throw new InvalidAssessmentRequestException("Submit each assessment question only once.");
            }
            if (submitted.selectedOptionIndex() < 0 || submitted.selectedOptionIndex() > 3) {
                throw new InvalidAssessmentRequestException("Option indexes must be between 0 and 3.");
            }
        }
        if (selectedOptions.size() != questionsById.size()) {
            throw new InvalidAssessmentRequestException("Submit exactly one answer for every assessment question.");
        }

        int correctCount = (int) questions.stream()
                .filter(question -> question.getCorrectOptionIndex() == selectedOptions.get(question.getId()))
                .count();
        int scorePercent = Math.round(correctCount * 100.0f / questions.size());
        int attemptNumber = attemptRepository.findFirstByAssessment_IdOrderByAttemptNumberDesc(assessmentId)
                .map(AssessmentAttempt::getAttemptNumber)
                .orElse(0) + 1;
        AssessmentAttempt attempt = attemptRepository.save(new AssessmentAttempt(assessment, attemptNumber, scorePercent));
        List<AssessmentAnswer> answers = questions.stream()
                .map(question -> new AssessmentAnswer(
                        attempt,
                        question,
                        selectedOptions.get(question.getId()),
                        question.getCorrectOptionIndex() == selectedOptions.get(question.getId())
                ))
                .toList();
        answerRepository.saveAll(answers);
        return toAttemptResponse(attempt, questions, answers);
    }

    @Transactional(readOnly = true)
    public AssessmentResultsResponse getResults(UUID assessmentId) {
        requireAssessment(assessmentId);
        List<AssessmentAttemptResultResponse> results = attemptRepository
                .findAllByAssessment_IdOrderByAttemptNumberDesc(assessmentId).stream()
                .map(attempt -> {
                    List<AssessmentQuestion> questions = questionRepository
                            .findAllByAssessment_IdOrderByQuestionOrderAsc(assessmentId);
                    List<AssessmentAnswer> answers = answerRepository
                            .findAllByAttempt_IdOrderByQuestion_QuestionOrderAsc(attempt.getId());
                    return toAttemptResponse(attempt, questions, answers);
                })
                .toList();
        return new AssessmentResultsResponse(assessmentId, results);
    }

    private Assessment requireAssessment(UUID assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
    }

    private AssessmentResponse toAssessmentResponse(Assessment assessment, List<AssessmentQuestion> questions) {
        List<AssessmentQuestionResponse> questionResponses = questions.stream()
                .map(question -> new AssessmentQuestionResponse(
                        question.getId(),
                        question.getQuestionOrder(),
                        question.getTopicName(),
                        question.getQuestionText(),
                        question.getOptions(),
                        question.getDifficulty(),
                        question.getSourcePage()
                ))
                .toList();
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getProject().getId(),
                assessment.getMaterial().getId(),
                assessment.getTitle(),
                assessment.getProvider(),
                assessment.getCreatedAt(),
                questionResponses
        );
    }

    private AssessmentAttemptResultResponse toAttemptResponse(
            AssessmentAttempt attempt,
            List<AssessmentQuestion> questions,
            List<AssessmentAnswer> answers
    ) {
        Map<UUID, AssessmentAnswer> answersByQuestion = new LinkedHashMap<>();
        answers.forEach(answer -> answersByQuestion.put(answer.getQuestion().getId(), answer));
        Map<String, int[]> topicCounts = new LinkedHashMap<>();
        for (AssessmentQuestion question : questions) {
            AssessmentAnswer answer = answersByQuestion.get(question.getId());
            if (answer == null) {
                continue;
            }
            int[] count = topicCounts.computeIfAbsent(question.getTopicName(), ignored -> new int[2]);
            count[1]++;
            if (answer.isCorrect()) {
                count[0]++;
            }
        }
        List<TopicAssessmentScoreResponse> topicScores = topicCounts.entrySet().stream()
                .map(entry -> {
                    int correct = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    int percent = Math.round(correct * 100.0f / total);
                    return new TopicAssessmentScoreResponse(entry.getKey(), correct, total, percent, percent < 60);
                })
                .toList();
        List<AssessmentQuestionFeedbackResponse> feedback = questions.stream()
                .map(question -> {
                    AssessmentAnswer answer = answersByQuestion.get(question.getId());
                    if (answer == null) {
                        return null;
                    }
                    int correctIndex = question.getCorrectOptionIndex();
                    return new AssessmentQuestionFeedbackResponse(
                            question.getId(),
                            question.getTopicName(),
                            question.getQuestionText(),
                            question.getOptions(),
                            answer.getSelectedOptionIndex(),
                            answer.isCorrect(),
                            correctIndex,
                            question.getOption(correctIndex),
                            question.getExplanation(),
                            question.getSourcePage()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
        int correctCount = (int) answers.stream().filter(AssessmentAnswer::isCorrect).count();
        return new AssessmentAttemptResultResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getScorePercent(),
                correctCount,
                questions.size(),
                attempt.getSubmittedAt(),
                topicScores,
                feedback
        );
    }

    private String assessmentTitle(String filename) {
        String title = "Diagnostic assessment: " + filename;
        return title.length() <= 200 ? title : title.substring(0, 200);
    }
}

package com.aitutor.backend;

import com.aitutor.backend.document.service.MaterialStorage;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.MaterialRepository;
import com.aitutor.backend.repository.TopicMasteryRepository;
import com.aitutor.backend.repository.AppUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.material-storage.directory=target/test-materials",
        "app.ai.provider=mock",
        "spring.ai.model.chat=mock",
        "spring.ai.model.embedding=mock",
        "app.learning.mastery-threshold=80",
        "app.learning.review-threshold=60"
})
@AutoConfigureMockMvc(addFilters = false)
class LearningPlanApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearningProjectRepository projectRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private TopicMasteryRepository masteryRepository;

    @Autowired
    private MaterialStorage storage;

    private LearningProject project;

    @BeforeEach
    void createProject() {
        project = projectRepository.save(new LearningProject("Learning plan API test", null, userRepository.findByUsername("demo").orElseThrow()));
    }

    @AfterEach
    void cleanUp() {
        for (Material material : materialRepository.findAllByProject_Id(project.getId())) {
            storage.delete(material.getId());
        }
        projectRepository.deleteById(project.getId());
    }

    @Test
    void createsGroundedPlanLessonsAndExercisesFromDiagnosticResults() throws Exception {
        String materialId = uploadMaterial();
        String assessmentId = createAssessment(materialId, 2);
        submitDiagnostic(assessmentId, 2, 0);

        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/learning-plans", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assessmentId\":\"" + assessmentId
                                + "\",\"availableStudyMinutes\":90,\"learningPreference\":\"examples first\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assessmentId").value(assessmentId))
                .andExpect(jsonPath("$.availableStudyMinutes").value(90))
                .andExpect(jsonPath("$.masteryThreshold").value(80))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[1].prerequisiteMethod").value("SOURCE_ORDER_FALLBACK"))
                .andExpect(jsonPath("$.items[1].prerequisites[0]").value("Blockchain Architecture"))
                .andExpect(jsonPath("$.items[0].lesson.explanation").isNotEmpty())
                .andExpect(jsonPath("$.items[0].lesson.workedExample").value(org.hamcrest.Matchers.containsString("page 1")))
                .andExpect(jsonPath("$.items[0].lesson.exercises.length()").value(1))
                .andExpect(jsonPath("$.items[0].lesson.exercises[0].options.length()").value(4))
                .andExpect(jsonPath("$.items[0].lesson.exercises[0].correctOptionIndex").doesNotExist())
                .andReturn();
        String planId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/learning-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
        mockMvc.perform(get("/api/v1/learning-plans/{planId}/lessons", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons.length()").value(2));

        String lessonId = JsonPath.read(result.getResponse().getContentAsString(), "$.items[0].lesson.id");
        mockMvc.perform(get("/api/v1/lessons/{lessonId}", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].correctOptionIndex").doesNotExist());
        mockMvc.perform(post("/api/v1/lessons/{lessonId}/exercises", lessonId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.options.length()").value(4))
                .andExpect(jsonPath("$.correctOptionIndex").doesNotExist());

        assertThat(masteryRepository.findAllByProject_Id(project.getId())).hasSize(2);
    }

    @Test
    void exerciseResultsUpdateMasteryAndChangeTheRecommendedAction() throws Exception {
        String materialId = uploadMaterial();
        String assessmentId = createAssessment(materialId, 1);
        submitDiagnostic(assessmentId, 1, 0);
        MvcResult planResult = createPlan(assessmentId);
        String planId = JsonPath.read(planResult.getResponse().getContentAsString(), "$.id");
        String exerciseId = JsonPath.read(planResult.getResponse().getContentAsString(), "$.items[0].lesson.exercises[0].id");

        String wrong = submitExercise(exerciseId, 1);
        assertThat(JsonPath.<String>read(wrong, "$.nextAction")).isEqualTo("REVISE");
        assertThat(JsonPath.<Integer>read(wrong, "$.masteryAfter")).isEqualTo(50);
        assertThat(JsonPath.<Boolean>read(wrong, "$.correct")).isFalse();

        String practice = submitExercise(exerciseId, 0);
        assertThat(JsonPath.<String>read(practice, "$.nextAction")).isEqualTo("PRACTICE");
        assertThat(JsonPath.<Integer>read(practice, "$.masteryAfter")).isEqualTo(67);

        submitExercise(exerciseId, 0);
        String advance = submitExercise(exerciseId, 0);
        assertThat(JsonPath.<String>read(advance, "$.nextAction")).isEqualTo("ADVANCE");
        assertThat(JsonPath.<Integer>read(advance, "$.masteryAfter")).isEqualTo(80);
        assertThat(JsonPath.<String>read(advance, "$.nextRecommendedTopic")).isNotBlank();

        mockMvc.perform(get("/api/v1/learning-plans/{planId}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("COMPLETED"));
        assertThat(masteryRepository.findAllByProject_Id(project.getId()).stream()
                .anyMatch(topicMastery -> topicMastery.getMasteryScore() == 80)).isTrue();

        mockMvc.perform(get("/api/v1/projects/{projectId}/progress", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topicsTracked").value(2))
                .andExpect(jsonPath("$.averageMasteryScore").isNumber())
                .andExpect(jsonPath("$.assessmentAttempts").value(1))
                .andExpect(jsonPath("$.exerciseAttempts").value(4))
                .andExpect(jsonPath("$.completedPlanItems").value(1))
                .andExpect(jsonPath("$.totalPlanItems").isNumber());
        mockMvc.perform(get("/api/v1/projects/{projectId}/mastery", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].masteryScore").isNumber())
                .andExpect(jsonPath("$[0].attempts").isNumber());
        mockMvc.perform(get("/api/v1/projects/{projectId}/recommendations", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").isNotEmpty());
        mockMvc.perform(get("/api/v1/projects/{projectId}/activity", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXERCISE_ATTEMPT"))
                .andExpect(jsonPath("$[4].type").value("ASSESSMENT_ATTEMPT"));
    }

    @Test
    void requiresSubmittedDiagnosticAndValidExerciseOption() throws Exception {
        String materialId = uploadMaterial();
        String assessmentId = createAssessment(materialId, 1);
        mockMvc.perform(post("/api/v1/projects/{projectId}/learning-plans", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assessmentId\":\"" + assessmentId + "\"}"))
                .andExpect(status().isBadRequest());

        submitDiagnostic(assessmentId, 1, 0);
        MvcResult plan = createPlan(assessmentId);
        String exerciseId = JsonPath.read(plan.getResponse().getContentAsString(),
                "$.items[0].lesson.exercises[0].id");
        mockMvc.perform(post("/api/v1/exercises/{exerciseId}/submit", exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionIndex\":4}"))
                .andExpect(status().isBadRequest());
    }

    private MvcResult createPlan(String assessmentId) throws Exception {
        return mockMvc.perform(post("/api/v1/projects/{projectId}/learning-plans", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assessmentId\":\"" + assessmentId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String submitExercise(String exerciseId, int optionIndex) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/exercises/{exerciseId}/submit", exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedOptionIndex\":" + optionIndex + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String createAssessment(String materialId, int count) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/{projectId}/assessments/diagnostic", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"materialId\":\"" + materialId + "\",\"questionCount\":" + count + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void submitDiagnostic(String assessmentId, int questionCount, int selectedOption) throws Exception {
        String assessment = mockMvc.perform(get("/api/v1/assessments/{assessmentId}", assessmentId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> questionIds = JsonPath.read(assessment, "$.questions[*].id");
        List<String> answers = new ArrayList<>();
        for (String questionId : questionIds) {
            answers.add("{\"questionId\":\"" + questionId
                    + "\",\"selectedOptionIndex\":" + selectedOption + "}");
        }
        mockMvc.perform(post("/api/v1/assessments/{assessmentId}/submit", assessmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":[" + String.join(",", answers) + "]}"))
                .andExpect(status().isOk());
    }

    private String uploadMaterial() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "blockchain-notes.pdf", MediaType.APPLICATION_PDF_VALUE,
                createPdf(
                        "Blockchain Architecture\nBitcoin blockchain records transactions in a public ledger.",
                        "Digital Cash Privacy\nBlind signatures let digital cash users prove payment without sharing identity."
                ));
        MvcResult result = mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/materials", project.getId()).file(pdf))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private byte[] createPdf(String... pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String pageText : pages) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float y = 750;
                    for (String line : pageText.split("\\R")) {
                        stream.beginText();
                        stream.setFont(font, 11);
                        stream.newLineAtOffset(50, y);
                        stream.showText(line);
                        stream.endText();
                        y -= 16;
                    }
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}

package com.aitutor.backend;

import com.aitutor.backend.document.service.MaterialStorage;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.MaterialRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.material-storage.directory=target/test-materials",
        "app.ai.provider=mock",
        "spring.ai.model.chat=mock",
        "spring.ai.model.embedding=mock"
})
@AutoConfigureMockMvc(addFilters = false)
class AiMaterialApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearningProjectRepository projectRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialStorage storage;

    private LearningProject project;

    @BeforeEach
    void createProject() {
        project = projectRepository.save(new LearningProject("AI API test", null, userRepository.findByUsername("demo").orElseThrow()));
    }

    @AfterEach
    void cleanUp() {
        for (Material material : materialRepository.findAllByProject_Id(project.getId())) {
            storage.delete(material.getId());
        }
        projectRepository.deleteById(project.getId());
    }

    @Test
    void mockProviderAnswersFromRetrievedMaterialAndReturnsPageSources() throws Exception {
        String materialId = uploadMaterial();

        mockMvc.perform(post("/api/v1/materials/{materialId}/ai/answer", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"How does Bitcoin blockchain record transactions?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("deterministic-mock"))
                .andExpect(jsonPath("$.retrievalMode").value("KEYWORD_OVERLAP"))
                .andExpect(jsonPath("$.answer").value(containsString("Bitcoin blockchain records transactions")))
                .andExpect(jsonPath("$.sources[0].pageNumber").value(1))
                .andExpect(jsonPath("$.sources[0].chunkId").isNotEmpty());
    }

    @Test
    void mockProviderProducesValidatedQuestionsAndCandidateTopics() throws Exception {
        String materialId = uploadMaterial();

        mockMvc.perform(post("/api/v1/materials/{materialId}/ai/questions", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"Bitcoin blockchain transactions\",\"count\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("deterministic-mock"))
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options.length()").value(4))
                .andExpect(jsonPath("$.questions[0].correctOptionIndex").doesNotExist())
                .andExpect(jsonPath("$.questions[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.questions[0].sourcePage").value(1));

        mockMvc.perform(post("/api/v1/materials/{materialId}/ai/topics", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("deterministic-mock"))
                .andExpect(jsonPath("$.retrievalMode").value("FULL_TEXT"))
                .andExpect(jsonPath("$.topics.length()").isNumber());
    }

    @Test
    void mockProviderDoesNotInventAnAnswerWhenNoChunkMatches() throws Exception {
        String materialId = uploadMaterial();

        mockMvc.perform(post("/api/v1/materials/{materialId}/ai/answer", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Explain photosynthesis in plants.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sources").isEmpty())
                .andExpect(jsonPath("$.answer").value(containsString("no passage that matches")));
    }

    @Test
    void rejectsInvalidQuestionGenerationRequest() throws Exception {
        String materialId = uploadMaterial();

        mockMvc.perform(post("/api/v1/materials/{materialId}/ai/questions", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topic\":\"Blockchain\",\"count\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private String uploadMaterial() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile(
                "file",
                "blockchain-notes.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                createPdf(
                        "Blockchain Architecture\nBitcoin blockchain records transactions in a public ledger.",
                        "Digital Cash Privacy\nBlind signatures let digital cash users prove payment without sharing identity."
                )
        );
        MvcResult result = mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/materials", project.getId()).file(pdf))
                .andExpect(status().isCreated())
                .andReturn();
        String materialId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        assertThat(materialId).isNotBlank();
        return materialId;
    }

    private byte[] createPdf(String... pageContents) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String pageContent : pageContents) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                    float y = 750;
                    for (String line : pageContent.split("\\R")) {
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

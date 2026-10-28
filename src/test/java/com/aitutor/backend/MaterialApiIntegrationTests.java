package com.aitutor.backend;

import com.aitutor.backend.document.service.MaterialStorage;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.AppUserRepository;
import com.aitutor.backend.repository.MaterialRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.material-storage.directory=target/test-materials")
@AutoConfigureMockMvc(addFilters = false)
class MaterialApiIntegrationTests {

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
        project = projectRepository.save(new LearningProject("Material API test", null, userRepository.findByUsername("demo").orElseThrow()));
    }

    @AfterEach
    void cleanUp() {
        for (Material material : materialRepository.findAllByProject_Id(project.getId())) {
            storage.delete(material.getId());
        }
        projectRepository.deleteById(project.getId());
    }

    @Test
    void uploadExtractsPageTextTopicsAndRetrievesMetadata() throws Exception {
        MockMultipartFile pdf = pdfFile(
                "biology.pdf",
                "Cell Structure\nCell membrane controls what enters and exits the cell.",
                "Mitochondria\nMitochondria generate energy for the cell."
        );

        MvcResult uploadResult = mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/materials", project.getId()).file(pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("biology.pdf"))
                .andExpect(jsonPath("$.status").value("PROCESSED"))
                .andExpect(jsonPath("$.pageCount").value(2))
                .andExpect(jsonPath("$.extractedCharCount").isNumber())
                .andReturn();
        String materialId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/projects/{projectId}/materials", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(materialId));

        mockMvc.perform(get("/api/v1/materials/{materialId}", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.material.status").value("PROCESSED"))
                .andExpect(jsonPath("$.chunks[0].pageNumber").value(1))
                .andExpect(jsonPath("$.chunks[0].content").value(containsString("Cell membrane")))
                .andExpect(jsonPath("$.chunks[1].pageNumber").value(2))
                .andExpect(jsonPath("$.chunks[1].content").value(containsString("Mitochondria")));

        mockMvc.perform(get("/api/v1/materials/{materialId}/topics", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(containsInAnyOrder("Cell Structure", "Mitochondria")));

        mockMvc.perform(post("/api/v1/materials/{materialId}/reprocess", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    @Test
    void longPageTextIsSplitIntoBoundedChunks() throws Exception {
        StringBuilder body = new StringBuilder("Cell Structure\n");
        for (int i = 0; i < 80; i++) {
            body.append("The cell membrane supports transport and communication in biological systems. ");
        }
        MvcResult uploadResult = mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/materials", project.getId())
                        .file(pdfFile("long-notes.pdf", body.toString())))
                .andExpect(status().isCreated())
                .andReturn();
        String materialId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        String detailJson = mockMvc.perform(get("/api/v1/materials/{materialId}", materialId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> chunkContents = JsonPath.read(detailJson, "$.chunks[*].content");
        assertThat(chunkContents).hasSizeGreaterThan(1)
                .allSatisfy(content -> assertThat(content.length()).isLessThanOrEqualTo(1500));
    }

    @Test
    void scannedOrBlankPdfIsStoredWithFailedProcessingStatus() throws Exception {
        MvcResult uploadResult = mockMvc.perform(multipart(
                        "/api/v1/projects/{projectId}/materials", project.getId())
                        .file(blankPdfFile()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(containsString("no extractable text")))
                .andReturn();
        String materialId = JsonPath.read(uploadResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/v1/materials/{materialId}", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunks").isEmpty());
    }

    @Test
    void rejectsFilesWithoutPdfExtensionAndSignature() throws Exception {
        MockMultipartFile notPdf = new MockMultipartFile(
                "file",
                "notes.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not a PDF".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/projects/{projectId}/materials", project.getId()).file(notPdf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Only PDF files")));

        assertThat(materialRepository.findAllByProject_Id(project.getId())).isEmpty();
    }

    private MockMultipartFile pdfFile(String filename, String... pages) throws IOException {
        return new MockMultipartFile(
                "file",
                filename,
                MediaType.APPLICATION_PDF_VALUE,
                createPdf(pages)
        );
    }

    private MockMultipartFile blankPdfFile() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            return new MockMultipartFile(
                    "file",
                    "scan.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    output.toByteArray()
            );
        }
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

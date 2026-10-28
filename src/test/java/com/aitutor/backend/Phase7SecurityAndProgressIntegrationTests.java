package com.aitutor.backend;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.AppUserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.bootstrap.password=LocalDevOnlyChangeMe123!")
@AutoConfigureMockMvc
class Phase7SecurityAndProgressIntegrationTests {
    private static final String DEMO_PASSWORD = "LocalDevOnlyChangeMe123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LearningProjectRepository projects;

    @Autowired
    private AppUserRepository users;

    private UUID createdProjectId;
    private String createdUsername;

    @AfterEach
    void cleanUp() {
        if (createdProjectId != null) projects.deleteById(createdProjectId);
        if (createdUsername != null) users.findByUsername(createdUsername).ifPresent(users::delete);
    }

    @Test
    void authScopesProjectsAndProgressEndpointsToTheOwner() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());

        String username = "phase7-" + UUID.randomUUID().toString().substring(0, 8);
        createdUsername = username;
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"AnotherLongPassword123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username));

        MvcResult created = mockMvc.perform(post("/api/v1/projects")
                        .with(httpBasic("demo", DEMO_PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Phase 7 security\",\"description\":\"Ownership check\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String projectId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        createdProjectId = UUID.fromString(projectId);

        mockMvc.perform(get("/api/v1/projects/{projectId}/progress", projectId)
                        .with(httpBasic("demo", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.topicsTracked").value(0))
                .andExpect(jsonPath("$.assessmentAttempts").value(0));

        mockMvc.perform(get("/api/v1/projects/{projectId}/recommendations", projectId)
                        .with(httpBasic("demo", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("CREATE_DIAGNOSTIC_ASSESSMENT"));

        mockMvc.perform(get("/api/v1/projects/{projectId}/mastery", projectId)
                        .with(httpBasic("demo", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());

        mockMvc.perform(get("/api/v1/projects/{projectId}/activity", projectId)
                        .with(httpBasic("demo", DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId)
                        .with(httpBasic(username, "AnotherLongPassword123!")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"));
    }
}

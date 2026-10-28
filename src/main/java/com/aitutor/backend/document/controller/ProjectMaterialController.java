package com.aitutor.backend.document.controller;

import com.aitutor.backend.document.service.MaterialService;
import com.aitutor.backend.dto.response.MaterialResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "basicAuth")
@RequestMapping("/api/v1/projects/{projectId}/materials")
public class ProjectMaterialController {

    private final MaterialService service;

    public ProjectMaterialController(MaterialService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialResponse> upload(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file
    ) {
        MaterialResponse created = service.upload(projectId, file);
        return ResponseEntity.created(URI.create("/api/v1/materials/" + created.id())).body(created);
    }

    @GetMapping
    public List<MaterialResponse> list(@PathVariable UUID projectId) {
        return service.listForProject(projectId);
    }
}

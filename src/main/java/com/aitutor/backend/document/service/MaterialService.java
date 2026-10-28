package com.aitutor.backend.document.service;

import com.aitutor.backend.document.exception.MaterialNotFoundException;
import com.aitutor.backend.document.exception.MaterialTooLargeException;
import com.aitutor.backend.document.exception.MaterialUploadException;
import com.aitutor.backend.dto.response.DocumentChunkResponse;
import com.aitutor.backend.dto.response.MaterialDetailResponse;
import com.aitutor.backend.dto.response.MaterialResponse;
import com.aitutor.backend.dto.response.TopicResponse;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.entity.Material;
import com.aitutor.backend.exception.ProjectNotFoundException;
import com.aitutor.backend.repository.DocumentChunkRepository;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.MaterialRepository;
import com.aitutor.backend.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MaterialService {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final LearningProjectRepository projectRepository;
    private final MaterialRepository materialRepository;
    private final DocumentChunkRepository chunkRepository;
    private final TopicRepository topicRepository;
    private final MaterialProcessingService processingService;
    private final MaterialStorage storage;
    private final long maxFileSizeBytes;

    public MaterialService(
            LearningProjectRepository projectRepository,
            MaterialRepository materialRepository,
            DocumentChunkRepository chunkRepository,
            TopicRepository topicRepository,
            MaterialProcessingService processingService,
            MaterialStorage storage,
            @Value("${app.materials.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.projectRepository = projectRepository;
        this.materialRepository = materialRepository;
        this.chunkRepository = chunkRepository;
        this.topicRepository = topicRepository;
        this.processingService = processingService;
        this.storage = storage;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public MaterialResponse upload(UUID projectId, MultipartFile file) {
        LearningProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        String originalFilename = sanitizeFilename(file == null ? null : file.getOriginalFilename());
        validateUpload(file, originalFilename);

        UUID materialId = UUID.randomUUID();
        Path storedPath = storage.store(materialId, file);
        Material material = new Material(
                materialId,
                project,
                originalFilename,
                storedPath.toString(),
                file.getSize()
        );
        try {
            materialRepository.saveAndFlush(material);
        } catch (RuntimeException exception) {
            storage.delete(materialId);
            throw exception;
        }
        return toResponse(processingService.process(materialId));
    }

    public List<MaterialResponse> listForProject(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }
        return materialRepository.findAllByProject_IdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public MaterialDetailResponse get(UUID materialId) {
        Material material = getMaterial(materialId);
        List<DocumentChunkResponse> chunks = chunkRepository.findAllByMaterial_IdOrderByChunkIndexAsc(materialId)
                .stream()
                .map(chunk -> new DocumentChunkResponse(
                        chunk.getChunkIndex(),
                        chunk.getPageNumber(),
                        chunk.getContent()
                ))
                .toList();
        return new MaterialDetailResponse(toResponse(material), chunks);
    }

    public List<TopicResponse> topics(UUID materialId) {
        getMaterial(materialId);
        return topicRepository.findAllByMaterial_IdOrderByNameAsc(materialId).stream()
                .map(topic -> new TopicResponse(
                        topic.getId(),
                        topic.getName(),
                        topic.getSourcePage(),
                        topic.getExtractionMethod()
                ))
                .toList();
    }

    public MaterialResponse reprocess(UUID materialId) {
        Material material = getMaterial(materialId);
        material.markProcessing();
        materialRepository.saveAndFlush(material);
        return toResponse(processingService.process(materialId));
    }

    private Material getMaterial(UUID materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new MaterialNotFoundException(materialId));
    }

    private void validateUpload(MultipartFile file, String filename) {
        if (file == null || file.isEmpty()) {
            throw new MaterialUploadException("A non-empty PDF file is required.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new MaterialTooLargeException("The PDF exceeds the 10 MB upload limit.");
        }
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new MaterialUploadException("Only PDF files are accepted.");
        }
        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(PDF_SIGNATURE.length);
            if (!java.util.Arrays.equals(signature, PDF_SIGNATURE)) {
                throw new MaterialUploadException("The uploaded file does not have a valid PDF signature.");
            }
        } catch (IOException exception) {
            throw new MaterialUploadException("The uploaded file could not be read.");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new MaterialUploadException("The uploaded PDF must have a filename.");
        }
        String normalized = originalFilename.replace('\\', '/');
        String filename = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        if (filename.isBlank()) {
            throw new MaterialUploadException("The uploaded PDF must have a filename.");
        }
        return filename.length() <= 255 ? filename : filename.substring(filename.length() - 255);
    }

    private MaterialResponse toResponse(Material material) {
        return new MaterialResponse(
                material.getId(),
                material.getProject().getId(),
                material.getOriginalFilename(),
                material.getFileSizeBytes(),
                material.getStatus(),
                material.getPageCount(),
                material.getExtractedCharCount(),
                material.getErrorMessage(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }
}

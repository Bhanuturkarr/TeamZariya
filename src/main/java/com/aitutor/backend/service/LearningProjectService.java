package com.aitutor.backend.service;

import com.aitutor.backend.dto.request.CreateProjectRequest;
import com.aitutor.backend.dto.response.ProjectResponse;
import com.aitutor.backend.entity.LearningProject;
import com.aitutor.backend.exception.ProjectNotFoundException;
import com.aitutor.backend.repository.LearningProjectRepository;
import com.aitutor.backend.repository.AppUserRepository;
import com.aitutor.backend.security.CurrentUsername;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LearningProjectService {

    private final LearningProjectRepository repository;
    private final AppUserRepository users;
    private final CurrentUsername currentUsername;

    public LearningProjectService(LearningProjectRepository repository, AppUserRepository users, CurrentUsername currentUsername) {
        this.repository = repository;
        this.users = users;
        this.currentUsername = currentUsername;
    }

    public ProjectResponse create(CreateProjectRequest request) {
        String name = request.name().trim();
        String description = request.description();
        if (description != null) {
            description = description.isBlank() ? null : description.trim();
        }
        var owner = users.findByUsername(currentUsername.get()).orElseThrow();
        LearningProject project = repository.save(new LearningProject(name, description, owner));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return repository.findAllByOwner_Username(currentUsername.get(), Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID projectId) {
        return repository.findByIdAndOwner_Username(projectId, currentUsername.get())
                .map(this::toResponse)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    public void delete(UUID projectId) {
        LearningProject project = repository.findByIdAndOwner_Username(projectId, currentUsername.get())
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        repository.delete(project);
    }

    private ProjectResponse toResponse(LearningProject project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}

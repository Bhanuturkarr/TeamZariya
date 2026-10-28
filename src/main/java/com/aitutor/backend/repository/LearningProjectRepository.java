package com.aitutor.backend.repository;

import com.aitutor.backend.entity.LearningProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface LearningProjectRepository extends JpaRepository<LearningProject, UUID> {
    List<LearningProject> findAllByOwner_Username(String username, org.springframework.data.domain.Sort sort);
    Optional<LearningProject> findByIdAndOwner_Username(UUID id, String username);
}

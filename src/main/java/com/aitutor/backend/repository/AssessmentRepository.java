package com.aitutor.backend.repository;

import com.aitutor.backend.entity.Assessment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
    List<Assessment> findAllByProject_Id(UUID projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assessment from Assessment assessment where assessment.id = :assessmentId")
    Optional<Assessment> findByIdForUpdate(@Param("assessmentId") UUID assessmentId);
}

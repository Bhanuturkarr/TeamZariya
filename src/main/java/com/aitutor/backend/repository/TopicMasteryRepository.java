package com.aitutor.backend.repository;

import com.aitutor.backend.entity.TopicMastery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicMasteryRepository extends JpaRepository<TopicMastery, UUID> {

    Optional<TopicMastery> findByProject_IdAndTopicName(UUID projectId, String topicName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select mastery from TopicMastery mastery where mastery.project.id = :projectId and mastery.topicName = :topicName")
    Optional<TopicMastery> findForUpdate(@Param("projectId") UUID projectId, @Param("topicName") String topicName);

    List<TopicMastery> findAllByProject_Id(UUID projectId);
}

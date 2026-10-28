package com.aitutor.backend.repository;

import com.aitutor.backend.entity.TopicPrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TopicPrerequisiteRepository extends JpaRepository<TopicPrerequisite, UUID> {

    List<TopicPrerequisite> findAllByTopic_Material_Id(UUID materialId);

    List<TopicPrerequisite> findAllByTopic_Id(UUID topicId);

    boolean existsByTopic_IdAndPrerequisiteTopic_Id(UUID topicId, UUID prerequisiteTopicId);
}

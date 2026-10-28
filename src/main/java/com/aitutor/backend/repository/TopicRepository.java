package com.aitutor.backend.repository;

import com.aitutor.backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByMaterial_IdOrderByNameAsc(UUID materialId);

    List<Topic> findAllByMaterial_IdOrderBySourcePageAscNameAsc(UUID materialId);

    void deleteAllByMaterial_Id(UUID materialId);
    List<Topic> findAllByMaterial_Project_IdOrderByNameAsc(UUID projectId);
}

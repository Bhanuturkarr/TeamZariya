package com.aitutor.backend.repository;

import com.aitutor.backend.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {

    List<Material> findAllByProject_IdOrderByCreatedAtDesc(UUID projectId);

    List<Material> findAllByProject_Id(UUID projectId);
}

package com.aitutor.backend.repository;

import com.aitutor.backend.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciseRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findAllByLesson_IdOrderByCreatedAtAsc(UUID lessonId);

    List<Exercise> findAllByLesson_IdInOrderByCreatedAtAsc(List<UUID> lessonIds);
}

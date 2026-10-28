package com.aitutor.backend.learning.dto.response;

import java.util.List;
import java.util.UUID;

public record LearningPlanLessonsResponse(UUID planId, List<LearningLessonResponse> lessons) {
}

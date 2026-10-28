package com.aitutor.backend.assessment.exception;

import java.util.UUID;

public class AssessmentNotFoundException extends RuntimeException {
    public AssessmentNotFoundException(UUID assessmentId) {
        super("Assessment " + assessmentId + " was not found.");
    }
}

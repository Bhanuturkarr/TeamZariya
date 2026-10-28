package com.aitutor.backend.assessment.exception;

public class InvalidAssessmentRequestException extends RuntimeException {
    public InvalidAssessmentRequestException(String message) {
        super(message);
    }
}

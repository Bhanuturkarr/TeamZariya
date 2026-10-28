package com.aitutor.backend.learning.exception;

public class LearningResourceNotFoundException extends RuntimeException {
    public LearningResourceNotFoundException(String resource) {
        super(resource + " was not found.");
    }
}

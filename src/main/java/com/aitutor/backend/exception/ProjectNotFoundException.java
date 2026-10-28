package com.aitutor.backend.exception;

import java.util.UUID;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(UUID projectId) {
        super("Learning project " + projectId + " was not found.");
    }
}

package com.aitutor.backend.document.exception;

import java.util.UUID;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(UUID materialId) {
        super("Material " + materialId + " was not found.");
    }
}

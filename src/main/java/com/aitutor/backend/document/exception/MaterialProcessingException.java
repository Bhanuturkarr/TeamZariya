package com.aitutor.backend.document.exception;

public class MaterialProcessingException extends RuntimeException {

    public MaterialProcessingException(String message) {
        super(message);
    }

    public MaterialProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

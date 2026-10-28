package com.aitutor.backend.exception;

import com.aitutor.backend.dto.response.ApiError;
import com.aitutor.backend.ai.exception.AiProviderException;
import com.aitutor.backend.ai.exception.AiOutputValidationException;
import com.aitutor.backend.document.exception.MaterialNotFoundException;
import com.aitutor.backend.document.exception.MaterialStorageException;
import com.aitutor.backend.document.exception.MaterialTooLargeException;
import com.aitutor.backend.document.exception.MaterialUploadException;
import com.aitutor.backend.assessment.exception.AssessmentNotFoundException;
import com.aitutor.backend.assessment.exception.InvalidAssessmentRequestException;
import com.aitutor.backend.learning.exception.LearningResourceNotFoundException;
import com.aitutor.backend.learning.exception.InvalidLearningRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({ProjectNotFoundException.class, MaterialNotFoundException.class, AssessmentNotFoundException.class,
            LearningResourceNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler({InvalidAssessmentRequestException.class, InvalidLearningRequestException.class})
    public ResponseEntity<ApiError> handleInvalidAssessmentRequest(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MaterialUploadException.class)
    public ResponseEntity<ApiError> handleInvalidUpload(
            MaterialUploadException exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler({MaterialTooLargeException.class, MaxUploadSizeExceededException.class})
    public ResponseEntity<ApiError> handleOversizedUpload(HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "The PDF exceeds the 10 MB upload limit.", request);
    }

    @ExceptionHandler(MaterialStorageException.class)
    public ResponseEntity<ApiError> handleStorageFailure(HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "The uploaded PDF could not be stored.", request);
    }

    @ExceptionHandler({AiProviderException.class, AiOutputValidationException.class})
    public ResponseEntity<ApiError> handleAiFailure(HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, "The AI service could not return a valid response.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "The request contains malformed or invalid values.", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}

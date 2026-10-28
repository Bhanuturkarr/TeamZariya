package com.aitutor.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.UUID;

@Component
public class OwnershipInterceptor implements HandlerInterceptor {
    private final ProjectOwnershipService ownership;
    private final CurrentUsername currentUsername;

    public OwnershipInterceptor(ProjectOwnershipService ownership, CurrentUsername currentUsername) {
        this.ownership = ownership;
        this.currentUsername = currentUsername;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object variablesObject = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(variablesObject instanceof Map<?, ?> variables)) return true;
        String username = currentUsername.get();
        check(variables, "projectId", ownership::requireProject, username);
        check(variables, "materialId", ownership::requireMaterial, username);
        check(variables, "assessmentId", ownership::requireAssessment, username);
        check(variables, "planId", ownership::requirePlan, username);
        check(variables, "lessonId", ownership::requireLesson, username);
        check(variables, "exerciseId", ownership::requireExercise, username);
        return true;
    }

    private void check(Map<?, ?> variables, String key, OwnershipCheck checker, String username) {
        Object rawId = variables.get(key);
        if (rawId != null) checker.check(UUID.fromString(rawId.toString()), username);
    }

    @FunctionalInterface
    private interface OwnershipCheck { void check(UUID id, String username); }
}

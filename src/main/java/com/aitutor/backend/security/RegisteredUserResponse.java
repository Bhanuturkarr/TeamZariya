package com.aitutor.backend.security;

import java.time.Instant;

public record RegisteredUserResponse(String username, Instant createdAt) { }

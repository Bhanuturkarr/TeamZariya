package com.aitutor.backend.security;

import com.aitutor.backend.entity.AppUser;
import com.aitutor.backend.repository.AppUserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthenticationController(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisteredUserResponse register(@Valid @RequestBody RegistrationRequest request) {
        String username = request.username().trim();
        if (!username.matches("[A-Za-z0-9._-]{3,80}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username may contain letters, numbers, dots, underscores, or hyphens.");
        }
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered.");
        }
        AppUser created = users.saveAndFlush(new AppUser(username, encoder.encode(request.password())));
        return new RegisteredUserResponse(created.getUsername(), created.getCreatedAt());
    }
}

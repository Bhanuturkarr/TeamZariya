package com.aitutor.backend.security;

import com.aitutor.backend.entity.AppUser;
import com.aitutor.backend.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapUser implements CommandLineRunner {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String password;

    public BootstrapUser(AppUserRepository users, PasswordEncoder encoder,
                         @Value("${app.security.bootstrap.password:LocalDevOnlyChangeMe123!}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String username = "demo";
        AppUser user = users.findByUsername(username).orElseGet(() -> new AppUser(username, encoder.encode(password)));
        user.changePasswordHash(encoder.encode(password));
        users.save(user);
    }
}

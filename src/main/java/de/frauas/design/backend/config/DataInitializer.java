package de.frauas.design.backend.config;

import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Seeds the database with a default admin account on first startup.
 *
 * <p>OAuth2 client registration has been removed: the token endpoint
 * ({@code POST /oauth2/user/token}) does not require a registered client.
 * Use environment variables {@code ADMIN_EMAIL} / {@code ADMIN_PASSWORD} to
 * override the default credentials.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@design.local}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:Admin123!}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createDefaultAdmin();
    }

    private void createDefaultAdmin() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Admin admin = new Admin();
            admin.setGuid(UUID.randomUUID().toString());
            admin.setName("Default Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Created default admin: {}", adminEmail);
        }
    }
}

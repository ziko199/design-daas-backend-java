package de.frauas.design.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application entry point.
 *
 * <p>{@code @EnableScheduling} activates the {@link de.frauas.design.backend.auth.TokenCleanupService}
 * scheduled job (SEC-M7).</p>
 */
@SpringBootApplication
@EnableScheduling
public class DesignBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(DesignBackendApplication.class, args);
    }
}



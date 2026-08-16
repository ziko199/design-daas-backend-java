package de.frauas.design.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DesignBackendApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test: verifies the Spring application context starts without errors.
    }
}

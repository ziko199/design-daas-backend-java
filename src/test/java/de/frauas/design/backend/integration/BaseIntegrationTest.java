package de.frauas.design.backend.integration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for all {@code @SpringBootTest} integration tests that need
 * a fully-configured {@link MockMvc} instance.
 *
 * <p>Spring Boot 4.x removed {@code @AutoConfigureMockMvc} from
 * {@code spring-boot-test-autoconfigure} — the annotation no longer registers
 * a {@code MockMvc} bean automatically.  This base class rebuilds the same
 * behaviour by constructing {@code MockMvc} from the {@code WebApplicationContext}
 * with the Spring Security filter chain applied (via
 * {@code SecurityMockMvcConfigurers.springSecurity()}).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseIntegrationTest {

    /** Fully-configured MockMvc shared by every subclass test method. */
    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @BeforeAll
    void initMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }
}

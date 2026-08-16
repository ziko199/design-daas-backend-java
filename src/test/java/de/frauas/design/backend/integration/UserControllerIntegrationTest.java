package de.frauas.design.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.UUID;

import static de.frauas.design.backend.integration.MockJwt.adminJwt;
import static de.frauas.design.backend.integration.MockJwt.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the User REST endpoints.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserController Integration")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    // Spring Boot 4.x removed JacksonAutoConfiguration — ObjectMapper is no longer a Spring bean.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_EMAIL = "ctrl.user@example.com";

    @BeforeAll
    void cleanup() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    // -------------------------------------------------------------------------
    // POST /user – public registration
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("POST /user – creates user with valid payload returns 200")
    void createUser_validPayload_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Ctrl User",
                "email", TEST_EMAIL,
                "password", "Password1"
        ));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.role").value("user"));
    }

    @Test
    @Order(2)
    @DisplayName("POST /user – duplicate email returns 400")
    void createUser_duplicateEmail_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Ctrl User",
                "email", TEST_EMAIL,
                "password", "Password1"
        ));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("POST /user – invalid email format returns 400")
    void createUser_invalidEmail_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "X",
                "email", "not-an-email",
                "password", "Password1"
        ));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("POST /user – blank name returns 400")
    void createUser_blankName_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "",
                "email", "new@example.com",
                "password", "Password1"
        ));

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // POST /user/validate_email – public
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("POST /user/validate_email – wrong code returns 400")
    void validateEmail_wrongCode_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", TEST_EMAIL,
                "registration_code", "WRONGCOD"
        ));

        mockMvc.perform(post("/user/validate_email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("POST /user/validate_email – correct code enables user and returns 200")
    void validateEmail_correctCode_returns200() throws Exception {
        User u = (User) userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        String code = u.getRegistrationCode();

        String body = objectMapper.writeValueAsString(Map.of(
                "email", TEST_EMAIL,
                "registration_code", code
        ));

        mockMvc.perform(post("/user/validate_email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        User updated = (User) userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.isEnabled()).isTrue();
    }

    // -------------------------------------------------------------------------
    // GET /users – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("GET /users – returns 200 with admin JWT")
    void getAllUsers_adminJwt_returns200() throws Exception {
        mockMvc.perform(get("/users")
                        .with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(8)
    @DisplayName("GET /users – returns 403 with user JWT")
    void getAllUsers_userJwt_returns403() throws Exception {
        mockMvc.perform(get("/users")
                        .with(userJwt(1)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    @DisplayName("GET /users – returns 401 with no JWT")
    void getAllUsers_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /user/{userId} – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("GET /user/{id} – returns 404 for unknown ID")
    void getUserById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/user/999999")
                        .with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(11)
    @DisplayName("GET /user/{id} – returns user for known ID")
    void getUserById_knownId_returns200() throws Exception {
        User u = (User) userRepository.findByEmail(TEST_EMAIL).orElseThrow();

        mockMvc.perform(get("/user/" + u.getId())
                        .with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    // -------------------------------------------------------------------------
    // PATCH /user/{userId} – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("PATCH /user/{id} – updates name")
    void updateUser_updatesName() throws Exception {
        User u = (User) userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        String body = objectMapper.writeValueAsString(Map.of("name", "Updated Name"));

        mockMvc.perform(patch("/user/" + u.getId())
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    // -------------------------------------------------------------------------
    // POST /user/{userId}/enable  &  /disable – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(13)
    @DisplayName("POST /user/{id}/disable then /enable toggles enabled state")
    void enableDisable_togglesState() throws Exception {
        User u = (User) userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        Integer id = u.getId();

        mockMvc.perform(post("/user/" + id + "/disable").with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(post("/user/" + id + "/enable").with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    // -------------------------------------------------------------------------
    // DELETE /user/{userId} – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(14)
    @DisplayName("DELETE /user/{id} – deletes user successfully")
    void deleteUser_success() throws Exception {
        User u = (User) userRepository.findByEmail(TEST_EMAIL).orElseThrow();

        mockMvc.perform(delete("/user/" + u.getId())
                        .with(adminJwt(999)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail(TEST_EMAIL)).isEmpty();
    }

    @Test
    @Order(15)
    @DisplayName("DELETE /user/{id} – returns 404 when already deleted")
    void deleteUser_alreadyDeleted_returns404() throws Exception {
        mockMvc.perform(delete("/user/999999")
                        .with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }
}


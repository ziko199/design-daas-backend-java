package de.frauas.design.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static de.frauas.design.backend.integration.MockJwt.adminJwt;
import static de.frauas.design.backend.integration.MockJwt.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Admin REST endpoints ({@code /admins}, {@code /admin/**}).
 * All endpoints require the {@code SCOPE_admin} authority.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("AdminController Integration")
class AdminControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_EMAIL = "ctrl.admin@example.com";

    @BeforeAll
    void cleanup() {
        userRepository.findByEmail(TEST_EMAIL).ifPresent(userRepository::delete);
    }

    @Test
    @Order(1)
    @DisplayName("POST /admin – no JWT returns 401")
    void createAdmin_noJwt_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Ctrl Admin",
                "email", TEST_EMAIL,
                "password", "Password1"
        ));

        mockMvc.perform(post("/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("POST /admin – user JWT returns 403")
    void createAdmin_userJwt_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Ctrl Admin",
                "email", TEST_EMAIL,
                "password", "Password1"
        ));

        mockMvc.perform(post("/admin")
                        .with(userJwt(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("POST /admin – admin JWT creates enabled admin, returns 200")
    void createAdmin_adminJwt_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Ctrl Admin",
                "email", TEST_EMAIL,
                "password", "Password1"
        ));

        mockMvc.perform(post("/admin")
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /admins – returns list including created admin")
    void getAllAdmins_returns200() throws Exception {
        mockMvc.perform(get("/admins").with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.email=='" + TEST_EMAIL + "')]").exists());
    }

    @Test
    @Order(5)
    @DisplayName("GET /admin/{id} – returns known admin")
    void getAdminById_knownId_returns200() throws Exception {
        Admin admin = (Admin) userRepository.findByEmail(TEST_EMAIL).orElseThrow();

        mockMvc.perform(get("/admin/" + admin.getId()).with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @Order(6)
    @DisplayName("GET /admin/{id} – returns 404 for unknown id")
    void getAdminById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/admin/999999").with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("DELETE /admin/{id} – deletes admin successfully")
    void deleteAdmin_success() throws Exception {
        Admin admin = (Admin) userRepository.findByEmail(TEST_EMAIL).orElseThrow();

        mockMvc.perform(delete("/admin/" + admin.getId()).with(adminJwt(999)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(userRepository.findByEmail(TEST_EMAIL)).isEmpty();
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /admin/{id} – returns 404 when already deleted")
    void deleteAdmin_alreadyDeleted_returns404() throws Exception {
        mockMvc.perform(delete("/admin/999999").with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }
}

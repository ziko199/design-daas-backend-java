package de.frauas.design.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static de.frauas.design.backend.integration.MockJwt.adminJwt;
import static de.frauas.design.backend.integration.MockJwt.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DesktopController and VersionController.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Desktop + Version Controller Integration")
class DesktopControllerIntegrationTest extends BaseIntegrationTest {

    // Spring Boot 4.x removed JacksonAutoConfiguration — ObjectMapper is no longer a Spring bean.
    // Construct directly for request-body serialization.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private Integer createdDesktopId;

    // -------------------------------------------------------------------------
    // GET /version – public
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("GET /version – returns 200 without auth")
    void version_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // POST /desktop – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(2)
    @DisplayName("POST /desktop – creates desktop with admin JWT")
    void createDesktop_adminJwt_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "IntegrationDesktop",
                "description", "Integration Test Desktop"
        ));

        String response = mockMvc.perform(post("/desktop")
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IntegrationDesktop"))
                .andReturn().getResponse().getContentAsString();

        // Extract created ID for subsequent tests
        createdDesktopId = objectMapper.readTree(response).get("id").asInt();
    }

    @Test
    @Order(3)
    @DisplayName("POST /desktop – returns 403 with user JWT")
    void createDesktop_userJwt_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "ShouldFail",
                "description", "Should not be created"
        ));

        mockMvc.perform(post("/desktop")
                        .with(userJwt(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @DisplayName("POST /desktop – returns 401 without JWT")
    void createDesktop_noJwt_returns401() throws Exception {
        mockMvc.perform(post("/desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /desktops – admin only
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("GET /desktops – returns array with admin JWT")
    void getAllDesktops_adminJwt_returns200() throws Exception {
        mockMvc.perform(get("/desktops").with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(6)
    @DisplayName("GET /desktops – returns 401 without JWT")
    void getAllDesktops_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/desktops"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // GET /desktop/{id}
    // -------------------------------------------------------------------------

    @Test
    @Order(7)
    @DisplayName("GET /desktop/{id} – returns 404 for unknown ID")
    void getDesktopById_unknown_returns404() throws Exception {
        mockMvc.perform(get("/desktop/999999").with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    @DisplayName("GET /desktop/{id} – returns desktop for known ID")
    void getDesktopById_known_returns200() throws Exception {
        if (createdDesktopId == null) return;
        mockMvc.perform(get("/desktop/" + createdDesktopId).with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdDesktopId));
    }

    // -------------------------------------------------------------------------
    // PUT /desktop/{id}
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("PUT /desktop/{id} – updates desktop")
    void updateDesktop_admin_returns200() throws Exception {
        if (createdDesktopId == null) return;
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "UpdatedDesktop",
                "description", "Updated"
        ));

        mockMvc.perform(put("/desktop/" + createdDesktopId)
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("UpdatedDesktop"));
    }

    // -------------------------------------------------------------------------
    // DELETE /desktop/{id}
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("DELETE /desktop/{id} – deletes desktop")
    void deleteDesktop_admin_returns200() throws Exception {
        if (createdDesktopId == null) return;
        mockMvc.perform(delete("/desktop/" + createdDesktopId).with(adminJwt(999)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(11)
    @DisplayName("DELETE /desktop/{id} – returns 404 for already deleted")
    void deleteDesktop_alreadyDeleted_returns404() throws Exception {
        if (createdDesktopId == null) return;
        mockMvc.perform(delete("/desktop/" + createdDesktopId).with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }
}


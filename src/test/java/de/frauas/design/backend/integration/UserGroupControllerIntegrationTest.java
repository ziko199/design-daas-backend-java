package de.frauas.design.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Map;

import static de.frauas.design.backend.integration.MockJwt.adminJwt;
import static de.frauas.design.backend.integration.MockJwt.userJwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the UserGroup REST endpoints ({@code /user_group(s)}).
 * All endpoints require the {@code SCOPE_admin} authority.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserGroupController Integration")
class UserGroupControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired UserGroupRepository userGroupRepository;
    @Autowired DesktopGroupRepository desktopGroupRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GROUP_NAME = "ctrl-test-group";
    private static final String DESKTOP_GROUP_NAME = "ctrl-test-desktop-group";

    @BeforeAll
    void cleanup() {
        userGroupRepository.findByName(GROUP_NAME).ifPresent(userGroupRepository::delete);
    }

    @Test
    @Order(1)
    @DisplayName("POST /user_group – no JWT returns 401")
    void createUserGroup_noJwt_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", GROUP_NAME));

        mockMvc.perform(post("/user_group")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    @DisplayName("POST /user_group – user JWT returns 403")
    void createUserGroup_userJwt_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", GROUP_NAME));

        mockMvc.perform(post("/user_group")
                        .with(userJwt(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    @DisplayName("POST /user_group – admin JWT creates group, returns 200")
    void createUserGroup_adminJwt_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", GROUP_NAME,
                "description", "Created by integration test"
        ));

        mockMvc.perform(post("/user_group")
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(GROUP_NAME));
    }

    @Test
    @Order(4)
    @DisplayName("POST /user_group – blank name falls back to description")
    void createUserGroup_blankName_fallsBackToDescription() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "",
                "description", "fallback-desc-group"
        ));

        mockMvc.perform(post("/user_group")
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("fallback-desc-group"));

        userGroupRepository.findByName("fallback-desc-group").ifPresent(userGroupRepository::delete);
    }

    @Test
    @Order(5)
    @DisplayName("POST /user_group – blank name and description returns 400")
    void createUserGroup_blankNameAndDescription_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", "", "description", ""));

        mockMvc.perform(post("/user_group")
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(6)
    @DisplayName("GET /user_groups – returns list including created group")
    void getAllUserGroups_returns200() throws Exception {
        mockMvc.perform(get("/user_groups").with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name=='" + GROUP_NAME + "')]").exists());
    }

    @Test
    @Order(7)
    @DisplayName("GET /user_group/{id} – returns known group")
    void getUserGroupById_knownId_returns200() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();

        mockMvc.perform(get("/user_group/" + group.getId()).with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(GROUP_NAME));
    }

    @Test
    @Order(8)
    @DisplayName("GET /user_group/{id} – returns 404 for unknown id")
    void getUserGroupById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/user_group/999999").with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(9)
    @DisplayName("PATCH /user_group/{id} – updates description")
    void updateUserGroup_patch_updatesDescription() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();
        String body = objectMapper.writeValueAsString(Map.of("description", "Updated via PATCH"));

        mockMvc.perform(patch("/user_group/" + group.getId())
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated via PATCH"));
    }

    @Test
    @Order(10)
    @DisplayName("PUT /user_group/{id} – updates description (same partial-update semantics as PATCH)")
    void updateUserGroup_put_updatesDescription() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();
        String body = objectMapper.writeValueAsString(Map.of("description", "Updated via PUT"));

        mockMvc.perform(put("/user_group/" + group.getId())
                        .with(adminJwt(999))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated via PUT"))
                .andExpect(jsonPath("$.name").value(GROUP_NAME));
    }

    @Test
    @Order(11)
    @DisplayName("POST /user_group/{userGroupId}/{desktopGroupId} – associates desktop group")
    void associateDesktopGroup_success() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();
        DesktopGroup dg = new DesktopGroup();
        dg.setName(DESKTOP_GROUP_NAME);
        dg = desktopGroupRepository.save(dg);

        mockMvc.perform(post("/user_group/" + group.getId() + "/" + dg.getId())
                        .with(adminJwt(999)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(DESKTOP_GROUP_NAME));
    }

    @Test
    @Order(12)
    @DisplayName("POST /user_group/{userGroupId}/{desktopGroupId} – already associated returns 409")
    void associateDesktopGroup_alreadyAssociated_returns409() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();
        DesktopGroup dg = desktopGroupRepository.findAll().stream()
                .filter(d -> DESKTOP_GROUP_NAME.equals(d.getName()))
                .findFirst().orElseThrow();

        mockMvc.perform(post("/user_group/" + group.getId() + "/" + dg.getId())
                        .with(adminJwt(999)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /user_group/{userGroupId}/{desktopGroupId} – removes association")
    void disassociateDesktopGroup_success() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();
        DesktopGroup dg = desktopGroupRepository.findAll().stream()
                .filter(d -> DESKTOP_GROUP_NAME.equals(d.getName()))
                .findFirst().orElseThrow();

        mockMvc.perform(delete("/user_group/" + group.getId() + "/" + dg.getId())
                        .with(adminJwt(999)))
                .andExpect(status().isOk());

        desktopGroupRepository.delete(dg);
    }

    @Test
    @Order(14)
    @DisplayName("DELETE /user_group/{id} – deletes group successfully")
    void deleteUserGroup_success() throws Exception {
        UserGroup group = userGroupRepository.findByName(GROUP_NAME).orElseThrow();

        mockMvc.perform(delete("/user_group/" + group.getId()).with(adminJwt(999)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(userGroupRepository.findByName(GROUP_NAME)).isEmpty();
    }

    @Test
    @Order(15)
    @DisplayName("DELETE /user_group/{id} – returns 404 when already deleted")
    void deleteUserGroup_alreadyDeleted_returns404() throws Exception {
        mockMvc.perform(delete("/user_group/999999").with(adminJwt(999)))
                .andExpect(status().isNotFound());
    }
}

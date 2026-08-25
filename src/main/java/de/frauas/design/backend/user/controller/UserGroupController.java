package de.frauas.design.backend.user.controller;

import de.frauas.design.backend.user.dto.UserGroupDto;
import de.frauas.design.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for user-group CRUD and desktop-group association management.
 * All endpoints require the {@code SCOPE_admin} authority (enforced class-wide).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class UserGroupController {

    private final UserService userService;

    @GetMapping("/user_groups")
    public ResponseEntity<List<UserGroupDto>> getAllUserGroups() {
        log.info("GET /user_groups — listing all user groups");
        List<UserGroupDto> groups = userService.getAllUserGroups();
        log.debug("GET /user_groups — returning {} groups", groups.size());
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/user_group")
    public ResponseEntity<UserGroupDto> createUserGroup(@RequestBody UserGroupDto request) {
        log.info("POST /user_group — creating user group name={}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(request.getDescription());
        }
        UserGroupDto result = userService.createUserGroup(request);
        log.info("POST /user_group — user group created id={} name={}", result.getId(), result.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * Updates a user group. Both PUT and PATCH are intentionally mapped to the same
     * handler: this endpoint always performs a partial (null-safe) update — omitted
     * fields are left unchanged — regardless of which HTTP method is used.
     *
     * <p>Note: stacking {@code @PutMapping} and {@code @PatchMapping} on one method does
     * NOT work in Spring MVC (only the first {@code @RequestMapping} meta-annotation is
     * honoured, so PATCH would 405) — {@code @RequestMapping(method = {...})} must be
     * used instead to register both HTTP methods.</p>
     */
    @PatchMapping("/user_group/{id}")
    public ResponseEntity<UserGroupDto> updateUserGroup(@PathVariable Integer id, @RequestBody UserGroupDto request) {
        log.info("PATCH /user_group/{} — updating user group", id);
        UserGroupDto result = userService.updateUserGroup(id, request);
        log.info("PATCH /user_group/{} — updated successfully", id);
        return ResponseEntity.ok(result);
    }
}

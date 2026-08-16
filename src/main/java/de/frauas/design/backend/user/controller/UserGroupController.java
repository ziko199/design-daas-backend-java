package de.frauas.design.backend.user.controller;

import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.user.dto.UserGroupDto;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import de.frauas.design.backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class UserGroupController {

    private final UserService userService;
    private final UserGroupRepository userGroupRepository;
    private final DesktopGroupRepository desktopGroupRepository;

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

    @GetMapping("/user_group/{id}")
    public ResponseEntity<UserGroupDto> getUserGroupById(@PathVariable Integer id) {
        log.info("GET /user_group/{} — fetching user group", id);
        return ResponseEntity.ok(userService.getUserGroupById(id));
    }

    @PutMapping("/user_group/{id}")
    @PatchMapping("/user_group/{id}")
    public ResponseEntity<UserGroupDto> updateUserGroup(
            @PathVariable Integer id,
            @RequestBody UserGroupDto request) {
        log.info("PUT/PATCH /user_group/{} — updating user group", id);
        UserGroupDto result = userService.updateUserGroup(id, request);
        log.info("PUT/PATCH /user_group/{} — updated successfully", id);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/user_group/{id}")
    public ResponseEntity<Void> deleteUserGroup(@PathVariable Integer id) {
        log.info("DELETE /user_group/{} — deleting user group", id);
        userService.deleteUserGroup(id);
        log.info("DELETE /user_group/{} — deleted successfully", id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user_group/{userGroupId}/{desktopGroupId}")
    public ResponseEntity<?> associateDesktopGroup(
            @PathVariable Integer userGroupId,
            @PathVariable Integer desktopGroupId) {
        log.info("POST /user_group/{}/{} — associating desktop group", userGroupId, desktopGroupId);
        UserGroup ug = userGroupRepository.findById(userGroupId)
            .orElseThrow(() -> new NoSuchElementException("UserGroup not found: " + userGroupId));
        DesktopGroup dg = desktopGroupRepository.findById(desktopGroupId)
            .orElseThrow(() -> new NoSuchElementException("DesktopGroup not found: " + desktopGroupId));
        if (ug.getDesktopGroups().contains(dg)) {
            log.warn("POST /user_group/{}/{} — already associated (conflict)", userGroupId, desktopGroupId);
            return ResponseEntity.status(409).body("Desktop group already in user group");
        }
        ug.getDesktopGroups().add(dg);
        userGroupRepository.save(ug);
        log.info("POST /user_group/{}/{} — association created", userGroupId, desktopGroupId);
        List<DesktopGroupDto> updatedList = ug.getDesktopGroups().stream()
            .map(DesktopGroupDto::from)
            .toList();
        return ResponseEntity.ok(updatedList);
    }

    @DeleteMapping("/user_group/{userGroupId}/{desktopGroupId}")
    public ResponseEntity<Void> disassociateDesktopGroup(
            @PathVariable Integer userGroupId,
            @PathVariable Integer desktopGroupId) {
        log.info("DELETE /user_group/{}/{} — removing desktop group association", userGroupId, desktopGroupId);
        UserGroup ug = userGroupRepository.findById(userGroupId)
            .orElseThrow(() -> new NoSuchElementException("UserGroup not found: " + userGroupId));
        DesktopGroup dg = desktopGroupRepository.findById(desktopGroupId)
            .orElseThrow(() -> new NoSuchElementException("DesktopGroup not found: " + desktopGroupId));
        ug.getDesktopGroups().remove(dg);
        userGroupRepository.save(ug);
        log.info("DELETE /user_group/{}/{} — association removed", userGroupId, desktopGroupId);
        return ResponseEntity.ok().build();
    }
}

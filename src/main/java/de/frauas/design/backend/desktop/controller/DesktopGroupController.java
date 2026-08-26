package de.frauas.design.backend.desktop.controller;

import de.frauas.design.backend.desktop.dto.DesktopGroupDto;
import de.frauas.design.backend.desktop.model.DesktopGroup;
import de.frauas.design.backend.desktop.repository.DesktopGroupRepository;
import de.frauas.design.backend.desktop.service.DesktopService;
import de.frauas.design.backend.user.dto.UserGroupDto;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class DesktopGroupController {

    private final DesktopService desktopService;
    private final DesktopGroupRepository desktopGroupRepository;
    private final UserGroupRepository userGroupRepository;

    @GetMapping("/desktop_groups")
    public ResponseEntity<List<DesktopGroupDto>> getAllDesktopGroups() {
        log.info("GET /desktop_groups — listing all desktop groups");
        List<DesktopGroupDto> groups = desktopService.getAllDesktopGroups();
        log.debug("GET /desktop_groups — returning {} groups", groups.size());
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/desktop_group")
    public ResponseEntity<DesktopGroupDto> createDesktopGroup(@RequestBody DesktopGroupDto request) {
        log.info("POST /desktop_group — creating desktop group name={}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(request.getDescription());
        }
        DesktopGroupDto result = desktopService.createDesktopGroup(request);
        log.info("POST /desktop_group — created id={} name={}", result.getId(), result.getName());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/desktop_group/{id}/user_group/{userGroupId}")
    public ResponseEntity<?> addUserGroup(@PathVariable Integer id, @PathVariable Integer userGroupId) {
        log.info("POST /desktop_group/{}/user_group/{} — adding user group", id, userGroupId);
        DesktopGroup dg = desktopGroupRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException("DesktopGroup not found: " + id));
        UserGroup ug = userGroupRepository
                .findById(userGroupId)
                .orElseThrow(() -> new NoSuchElementException("UserGroup not found: " + userGroupId));
        if (ug.getDesktopGroups().contains(dg)) {
            log.warn("POST /desktop_group/{}/user_group/{} — already associated (conflict)", id, userGroupId);
            return ResponseEntity.status(409).body("User group already associated with this desktop group");
        }
        ug.getDesktopGroups().add(dg);
        userGroupRepository.save(ug);
        log.info("POST /desktop_group/{}/user_group/{} — association created", id, userGroupId);
        List<UserGroupDto> updatedUserGroups =
                dg.getUserGroups().stream().map(UserGroupDto::from).toList();
        return ResponseEntity.ok(updatedUserGroups);
    }
}

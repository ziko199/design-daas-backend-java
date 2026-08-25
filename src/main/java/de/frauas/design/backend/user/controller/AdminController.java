package de.frauas.design.backend.user.controller;

import de.frauas.design.backend.user.dto.AdminDto;
import de.frauas.design.backend.user.dto.CreateAdminRequest;
import de.frauas.design.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for admin CRUD. All endpoints require the {@code SCOPE_admin}
 * authority (enforced class-wide).
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class AdminController {

    private final UserService userService;

    @GetMapping("/admins")
    public ResponseEntity<List<AdminDto>> getAllAdmins() {
        log.info("GET /admins — listing all admins");
        List<AdminDto> admins = userService.getAllAdmins();
        log.debug("GET /admins — returning {} admins", admins.size());
        return ResponseEntity.ok(admins);
    }

    @PostMapping("/admin")
    public ResponseEntity<AdminDto> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        log.info("POST /admin — creating admin email={}", request.getEmail());
        AdminDto result = userService.createAdmin(request);
        log.info("POST /admin — admin created id={} email={}", result.getId(), result.getEmail());
        return ResponseEntity.ok(result);
    }
}

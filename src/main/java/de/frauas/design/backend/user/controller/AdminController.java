package de.frauas.design.backend.user.controller;

import de.frauas.design.backend.user.dto.AdminDto;
import de.frauas.design.backend.user.dto.CreateAdminRequest;
import de.frauas.design.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/admin/{adminId}")
    public ResponseEntity<AdminDto> getAdminById(@PathVariable Integer adminId) {
        log.info("GET /admin/{} — fetching admin", adminId);
        return ResponseEntity.ok(userService.getAdminById(adminId));
    }

    @PostMapping("/admin")
    public ResponseEntity<AdminDto> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        log.info("POST /admin — creating admin email={}", request.getEmail());
        AdminDto result = userService.createAdmin(request);
        log.info("POST /admin — admin created id={} email={}", result.getId(), result.getEmail());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/admin/{adminId}")
    public ResponseEntity<String> deleteAdmin(@PathVariable Integer adminId) {
        log.info("DELETE /admin/{} — deleting admin", adminId);
        userService.deleteAdmin(adminId);
        log.info("DELETE /admin/{} — admin deleted successfully", adminId);
        return ResponseEntity.ok("Admin deleted");
    }
}

package de.frauas.design.backend.user.controller;

import de.frauas.design.backend.user.dto.CreateUserRequest;
import de.frauas.design.backend.user.dto.PatchUserRequest;
import de.frauas.design.backend.user.dto.UserDto;
import de.frauas.design.backend.user.dto.ValidateEmailRequest;
import de.frauas.design.backend.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for self-service user registration/verification and admin-managed
 * user CRUD, plus the desktop-access-check and application-request endpoints used
 * by regular users.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("POST /user — registering new user email={}", request.getEmail());
        UserDto result = userService.createUser(request);
        log.info("POST /user — user created id={} email={}", result.getId(), result.getEmail());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/validate_email")
    public ResponseEntity<Void> validateEmail(@RequestBody ValidateEmailRequest request) {
        log.info("POST /user/validate_email — email={}", request.getEmail());
        boolean ok = userService.validateEmail(request.getEmail(), request.getRegistrationCode());
        if (ok) {
            log.info("POST /user/validate_email — email verified successfully email={}", request.getEmail());
        } else {
            log.warn("POST /user/validate_email — verification failed email={}", request.getEmail());
        }
        return ok ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    /**
     * GET /users — list users.
     * When no {@code page} parameter is given, returns ALL users (matching PHP behaviour).
     * When {@code page} is specified, {@code per_page} must also be present.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<List<UserDto>> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(name = "per_page", required = false) Integer perPage) {
        log.info("GET /users — page={} perPage={}", page, perPage);
        if (page == null) {
            List<UserDto> users = userService.getAllUsers();
            log.debug("GET /users — returning {} users (no pagination)", users.size());
            return ResponseEntity.ok(users);
        }
        if (perPage == null) {
            log.warn("GET /users — page specified without per_page");
            return ResponseEntity.badRequest().build();
        }
        List<UserDto> users = userService.getAllUsers(page, perPage);
        log.debug("GET /users — returning {} users (page={} perPage={})", users.size(), page, perPage);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable Integer userId, @Valid @RequestBody PatchUserRequest request) {
        log.info("PATCH /user/{} — updating user", userId);
        UserDto result = userService.updateUser(userId, request);
        log.info("PATCH /user/{} — user updated successfully", userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/{userId}/enable")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<UserDto> enableUser(@PathVariable Integer userId) {
        log.info("POST /user/{}/enable — enabling user", userId);
        UserDto result = userService.enableUser(userId);
        log.info("POST /user/{}/enable — user enabled", userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/{userId}/disable")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<UserDto> disableUser(@PathVariable Integer userId) {
        log.info("POST /user/{}/disable — disabling user", userId);
        UserDto result = userService.disableUser(userId);
        log.info("POST /user/{}/disable — user disabled", userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/user/request-application/{application}/{isoCode}")
    @PreAuthorize("hasAnyAuthority('SCOPE_user', 'SCOPE_expert', 'SCOPE_admin')")
    public ResponseEntity<Void> requestApplication(
            @PathVariable String application, @PathVariable String isoCode, @AuthenticationPrincipal Jwt jwt) {
        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("POST /user/request-application/{}/{} — userId={}", application, isoCode, userId);
        userService.requestApplication(userId, application);
        log.info(
                "POST /user/request-application — application request email sent userId={} app={}",
                userId,
                application);
        return ResponseEntity.ok().build();
    }
}

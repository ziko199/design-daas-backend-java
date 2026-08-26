package de.frauas.design.backend.user.service;

import de.frauas.design.backend.user.dto.AdminDto;
import de.frauas.design.backend.user.dto.CreateAdminRequest;
import de.frauas.design.backend.user.dto.CreateUserRequest;
import de.frauas.design.backend.user.dto.PatchUserRequest;
import de.frauas.design.backend.user.dto.UserDto;
import de.frauas.design.backend.user.dto.UserGroupDto;
import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.model.UserGroup;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for user, admin and user-group management.
 *
 * <p>Mirrors the behaviour of the legacy PHP {@code UserService} /
 * {@code UserGroupService} classes: registration with email verification,
 * password-policy enforcement, CRUD for {@link User}/{@link Admin}/{@link UserGroup},
 * and the application-access request flow.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /** Minimum 8 chars, at least one uppercase, one lowercase, one digit. */
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";
    /** Cryptographically secure random for registration code generation. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    // -------------------------------------------------------------------------
    // User CRUD
    // -------------------------------------------------------------------------

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        log.debug("createUser — email={}", request.getEmail());
        validatePassword(request.getPassword());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("createUser — email already in use: {}", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }
        User user = new User();
        user.setGuid(UUID.randomUUID().toString());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);
        user.setRegistrationCode(generateRegistrationCode());
        user.setRegistrationCodeTimeout(LocalDateTime.now().plusHours(24));
        if (request.getGroups() != null && !request.getGroups().isEmpty()) {
            user.setGroups(userGroupRepository.findAllById(request.getGroups()));
        }
        userRepository.save(user);
        log.info("createUser — user created id={} email={}", user.getId(), user.getEmail());
        mailService.sendRegistrationCode(user.getEmail(), user.getRegistrationCode());
        return UserDto.from(user);
    }

    @Transactional
    public boolean validateEmail(String email, String registrationCode) {
        log.debug("validateEmail — email={}", email);
        Optional<BaseUser> opt = userRepository.findByEmail(email);
        if (opt.isEmpty() || !(opt.get() instanceof User user)) {
            log.warn("validateEmail — no user found for email={}", email);
            return false;
        }
        if (user.getRegistrationCode() == null || !user.getRegistrationCode().equals(registrationCode)) {
            log.warn("validateEmail — invalid registration code for email={}", email);
            return false;
        }
        if (user.getRegistrationCodeTimeout() != null
                && LocalDateTime.now().isAfter(user.getRegistrationCodeTimeout())) {
            log.warn("validateEmail — registration code expired for email={}", email);
            return false;
        }
        user.setEnabled(true);
        user.setRegistrationUsedMoment(LocalDateTime.now());
        user.setRegistrationCode(null);
        userRepository.save(user);
        log.info("validateEmail — email verified for userId={} email={}", user.getId(), email);
        return true;
    }

    /**
     * Returns a paginated slice of all regular users, ordered by id.
     *
     * <p>Note: pagination is applied in-memory after loading all users (mirrors the
     * legacy PHP behaviour). This is acceptable for the expected data volume but should
     * be revisited (e.g. moved to a DB-level {@code LIMIT}/{@code OFFSET} query) if the
     * user table grows significantly.</p>
     *
     * @param page    zero-based page index; must be {@code >= 0}
     * @param perPage number of items per page; must be {@code > 0}
     * @throws IllegalArgumentException if {@code page < 0} or {@code perPage <= 0}
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers(int page, int perPage) {
        log.debug("getAllUsers — page={} perPage={}", page, perPage);
        if (page < 0 || perPage <= 0) {
            log.warn("getAllUsers — invalid pagination page={} perPage={}", page, perPage);
            throw new IllegalArgumentException("page must be >= 0 and per_page must be > 0");
        }
        List<User> all = userRepository.findAllUsers();
        int from = page * perPage;
        if (from >= all.size()) return List.of();
        int to = Math.min(from + perPage, all.size());
        return all.subList(from, to).stream().map(UserDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        log.debug("getAllUsers — no pagination");
        return userRepository.findAllUsers().stream().map(UserDto::from).toList();
    }

    @Transactional
    public UserDto updateUser(Integer id, PatchUserRequest request) {
        log.debug("updateUser — id={}", id);
        User user =
                userRepository.findUserById(id).orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        if (request.getName() != null) user.setName(request.getName());
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    log.warn("updateUser — email already in use: {}", request.getEmail());
                    throw new IllegalArgumentException("Email already in use");
                }
            });
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null) {
            validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getGroups() != null) {
            user.setGroups(userGroupRepository.findAllById(request.getGroups()));
        }
        userRepository.save(user);
        log.info("updateUser — user updated id={}", id);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto enableUser(Integer id) {
        log.debug("enableUser — id={}", id);
        User user =
                userRepository.findUserById(id).orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        user.setEnabled(true);
        userRepository.save(user);
        log.info("enableUser — user enabled id={}", id);
        return UserDto.from(user);
    }

    @Transactional
    public UserDto disableUser(Integer id) {
        log.debug("disableUser — id={}", id);
        User user =
                userRepository.findUserById(id).orElseThrow(() -> new NoSuchElementException("User not found: " + id));
        user.setEnabled(false);
        userRepository.save(user);
        log.info("disableUser — user disabled id={}", id);
        return UserDto.from(user);
    }

    // -------------------------------------------------------------------------
    // Admin CRUD
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminDto> getAllAdmins() {
        log.debug("getAllAdmins");
        return userRepository.findAllAdmins().stream().map(AdminDto::from).toList();
    }

    @Transactional
    public AdminDto createAdmin(CreateAdminRequest request) {
        log.debug("createAdmin — email={}", request.getEmail());
        validatePassword(request.getPassword());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("createAdmin — email already in use: {}", request.getEmail());
            throw new IllegalArgumentException("Email already in use");
        }
        Admin admin = new Admin();
        admin.setGuid(UUID.randomUUID().toString());
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("createAdmin — admin created id={} email={}", admin.getId(), admin.getEmail());
        return AdminDto.from(admin);
    }

    // -------------------------------------------------------------------------
    // UserGroup CRUD
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserGroupDto> getAllUserGroups() {
        log.debug("getAllUserGroups");
        return userGroupRepository.findAll().stream().map(UserGroupDto::from).toList();
    }

    /**
     * Creates a new user group.
     *
     * <p>If no name is given, the description is used as the name instead — at least one of the two must be present.</p>
     *
     * @param request the group data; {@code name} falls back to {@code description} when blank
     * @return the created group
     * @throws IllegalArgumentException if both {@code name} and {@code description} are blank
     */
    @Transactional
    public UserGroupDto createUserGroup(UserGroupDto request) {
        log.debug("createUserGroup — name={}", request.getName());
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(request.getDescription());
        }
        if (request.getName() == null || request.getName().isBlank()) {
            log.warn("createUserGroup — rejected: name and description both blank");
            throw new IllegalArgumentException("UserGroup requires a name or a description");
        }
        UserGroup group = new UserGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        userGroupRepository.save(group);
        log.info("createUserGroup — created id={} name={}", group.getId(), group.getName());
        return UserGroupDto.from(group);
    }

    @Transactional
    public UserGroupDto updateUserGroup(Integer id, UserGroupDto request) {
        log.debug("updateUserGroup — id={}", id);
        UserGroup group = userGroupRepository
                .findById(id)
                .orElseThrow(() -> new NoSuchElementException("UserGroup not found: " + id));
        if (request.getName() != null) group.setName(request.getName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());

        // Replace group members when userIds is supplied (mirrors PHP UserGroupService::update).
        // We must mutate via User.groups (the owning side of the ManyToMany).
        if (request.getUserIds() != null) {
            Set<Integer> desiredIds = new HashSet<>(request.getUserIds());

            // Users currently in this group
            List<User> current = new ArrayList<>(group.getUsers());
            Set<Integer> currentIds = current.stream().map(User::getId).collect(Collectors.toSet());

            // Load the full desired set (only User entities — admins have no groups)
            List<User> desired = userRepository.findAllById(desiredIds).stream()
                    .filter(u -> u instanceof User)
                    .map(u -> (User) u)
                    .toList();

            // Remove group from users who are no longer in the desired list
            for (User u : current) {
                if (!desiredIds.contains(u.getId())) {
                    u.getGroups().remove(group);
                    userRepository.save(u);
                    log.debug("updateUserGroup — removed userId={} from groupId={}", u.getId(), id);
                }
            }

            // Add group to newly listed users
            for (User u : desired) {
                if (!currentIds.contains(u.getId())) {
                    u.getGroups().add(group);
                    userRepository.save(u);
                    log.debug("updateUserGroup — added userId={} to groupId={}", u.getId(), id);
                }
            }
        }

        userGroupRepository.save(group);
        log.info("updateUserGroup — updated id={}", id);
        return UserGroupDto.from(group);
    }

    // -------------------------------------------------------------------------
    // Application request
    // -------------------------------------------------------------------------

    /**
     * Sends an application-request email on behalf of the authenticated user.
     * Mirrors PHP UserService::requestApplication().
     *
     * @param userId      the ID of the requesting user (from JWT subject)
     * @param application the application name the user wants access to
     */
    @Transactional(readOnly = true)
    public void requestApplication(Integer userId, String application) {
        log.debug("requestApplication — userId={} application={}", userId, application);
        var baseUser = userRepository
                .findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        mailService.sendApplicationRequestEmail(
                baseUser.getEmail(), baseUser.getName(), String.valueOf(baseUser.getId()), application);
        log.info("requestApplication — email sent for userId={} application={}", userId, application);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validatePassword(String password) {
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain uppercase, lowercase, and a digit");
        }
    }

    private String generateRegistrationCode() {
        String chars = "ABCDEF0123456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        return sb.toString();
    }
}

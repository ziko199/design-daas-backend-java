package de.frauas.design.backend.user.service;

import de.frauas.design.backend.shared.util.LogMasking;
import de.frauas.design.backend.user.dto.AdminDto;
import de.frauas.design.backend.user.dto.CreateAdminRequest;
import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core business logic for admin account management.
 *
 * <p>Unlike regular users ({@link UserService}), admins are enabled
 * immediately and are not subject to email verification.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountValidator accountValidator;

    /**
     * Returns every admin account.
     *
     * @return all admins
     */
    @Transactional(readOnly = true)
    public List<AdminDto> getAllAdmins() {
        log.debug("getAllAdmins");
        return userRepository.findAllAdmins().stream().map(AdminDto::from).toList();
    }

    /**
     * Creates a new admin account. Unlike regular users, admins are enabled immediately
     * and are not subject to email verification.
     *
     * @param request the new admin's details
     * @return the created admin
     * @throws de.frauas.design.backend.user.exception.WeakPasswordException if the password fails policy checks
     * @throws de.frauas.design.backend.user.exception.EmailAlreadyInUseException if the email is already registered
     */
    @Transactional
    public AdminDto createAdmin(CreateAdminRequest request) {
        log.debug("createAdmin — email={}", LogMasking.maskEmail(request.getEmail()));
        accountValidator.validatePassword(request.getPassword());
        accountValidator.assertEmailAvailable(request.getEmail(), null);
        Admin admin = new Admin();
        admin.setGuid(UUID.randomUUID().toString());
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        admin.setEnabled(true);
        userRepository.save(admin);
        log.info("createAdmin — admin created id={} email={}", admin.getId(), LogMasking.maskEmail(admin.getEmail()));
        return AdminDto.from(admin);
    }
}

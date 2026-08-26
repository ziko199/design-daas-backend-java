package de.frauas.design.backend.user.service;

import de.frauas.design.backend.shared.util.LogMasking;
import de.frauas.design.backend.user.exception.EmailAlreadyInUseException;
import de.frauas.design.backend.user.exception.WeakPasswordException;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Account-creation invariants shared by {@link UserService} and {@link AdminService}:
 * password-strength policy and email uniqueness.
 *
 * <p>Extracted so both services enforce identical rules without duplicating the checks.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountValidator {

    /** Minimum 8 chars, at least one uppercase, one lowercase, one digit. */
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";

    private final UserRepository userRepository;

    /**
     * Validates password strength.
     *
     * @param password the plain-text password to check
     * @throws WeakPasswordException if the password is {@code null} or fails the strength policy
     */
    public void validatePassword(String password) {
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            log.warn("validatePassword — rejected weak password");
            throw new WeakPasswordException();
        }
    }

    /**
     * Ensures no other account already uses {@code email}.
     *
     * @param email the email to check
     * @param excludingUserId if non-null, an existing account with this ID is allowed to
     *     already hold {@code email} (used when a user updates their own record without
     *     changing their address)
     * @throws EmailAlreadyInUseException if the email is already registered to a different account
     */
    public void assertEmailAvailable(String email, Integer excludingUserId) {
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(excludingUserId)) {
                log.warn("assertEmailAvailable — email already in use: {}", LogMasking.maskEmail(email));
                throw new EmailAlreadyInUseException();
            }
        });
    }
}

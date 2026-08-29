package de.frauas.design.backend.user.service;

import de.frauas.design.backend.shared.util.LogMasking;
import de.frauas.design.backend.shared.util.PasswordPolicy;
import de.frauas.design.backend.user.exception.EmailAlreadyInUseException;
import de.frauas.design.backend.user.exception.WeakPasswordException;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Enforces account-related business rules shared by user and administrator services.
 *
 * <p>Currently validates password strength and email uniqueness.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountValidator {

    private final UserRepository userRepository;

    /**
     * Validates the password against the configured password policy.
     *
     * @param password the plain-text password to validate
     * @throws WeakPasswordException if the password is {@code null} or does not meet the password policy
     */
    public void validatePassword(String password) {
        if (password == null || !password.matches(PasswordPolicy.PASSWORD_REGEX)) {
            log.warn("validatePassword — rejected weak password");
            throw new WeakPasswordException();
        }
    }

    /**
     * Ensures that the email address is not used by another account.
     *
     * @param email the email address to check
     * @param excludingUserId the ID of an account allowed to use the email
     *                        (Update scenario), or {@code null} when creating
     *                        a new account
     * @throws EmailAlreadyInUseException if the email is already used
     *          by another account
     */
    public void assertEmailAvailable(String email, Integer excludingUserId) {
        userRepository
                .findByEmail(email)
                .filter(existing -> !existing.getId().equals(excludingUserId))
                .ifPresent(existing -> {
                    log.warn("assertEmailAvailable — email already in use: {}", LogMasking.maskEmail(email));
                    throw new EmailAlreadyInUseException();
                });
    }
}

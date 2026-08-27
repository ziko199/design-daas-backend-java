package de.frauas.design.backend.user.validation;

/**
 * Single source of truth for the account password-strength policy: minimum 8
 * characters, at least one uppercase letter, one lowercase letter, and one digit.
 *
 * <p>Referenced both by {@link de.frauas.design.backend.user.service.AccountValidator}
 * (service-layer enforcement) and by the {@code @Pattern} constraints on
 * {@code CreateUserRequest}, {@code CreateAdminRequest}, and {@code PatchUserRequest}
 * (bean-validation-layer enforcement), so the two layers can never drift apart.</p>
 */
public final class PasswordPolicy {

    /** Minimum 8 chars, at least one uppercase, one lowercase, one digit. */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";

    private PasswordPolicy() {}
}

package de.frauas.design.backend.shared.util;

/**
 * Central definition of the password-strength policy used by the validation
 * layers.
 *
 * <p>The policy requires at least 8 characters, including one uppercase
 * letter, one lowercase letter, and one digit.</p>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    /**
     * Minimum 8 chars, at least one uppercase, one lowercase, one digit.
     */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$";

    private PasswordPolicy() {}
}

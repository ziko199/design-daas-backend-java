package de.frauas.design.backend.shared.util;

/**
 * Small helpers for redacting personally identifiable information (PII) before
 * it is written to application logs.
 */
public final class LogMasking {

    private LogMasking() {}

    /**
     * Masks an email address for logging, keeping only the first character of the
     * local part and the full domain, e.g. {@code "john.doe@example.com"} becomes
     * {@code "j***@example.com"}.
     *
     * @param email the raw email address, may be null/blank
     * @return the masked email, or the original value if it has no recognizable local/domain parts
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}

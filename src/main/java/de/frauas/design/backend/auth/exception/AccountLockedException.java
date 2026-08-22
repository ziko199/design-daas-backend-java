package de.frauas.design.backend.auth.exception;

/**
 * the account is temporarily locked after too many consecutive failed
 * login attempts.
 */
public class AccountLockedException extends TokenGrantException {
    public AccountLockedException() {
        super(429, "account_locked",
                "Account is temporarily locked due to too many failed attempts — try again later");
    }
}

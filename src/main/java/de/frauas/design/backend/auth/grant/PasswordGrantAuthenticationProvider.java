package de.frauas.design.backend.auth.grant;

/**
 * @deprecated Removed as part of SEC-C1 fix. The Spring Authorization Server
 *             password-grant path has been retired. The sole token endpoint is
 *             now {@code POST /oauth2/user/token} handled by
 *             {@link de.frauas.design.backend.auth.TokenController}.
 */
@Deprecated
final class PasswordGrantAuthenticationProvider {
    private PasswordGrantAuthenticationProvider() {}
}

package de.frauas.design.backend.auth.controller;

import de.frauas.design.backend.auth.dto.TokenGrantResultDto;
import de.frauas.design.backend.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single token endpoint at {@code POST /oauth2/user/token}.
 * Handles the resource-owner password grant and the refresh-token grant.
 *
 * <p>This controller is intentionally thin: it only parses the request and shapes
 * the response. All business logic — grant-type dispatch, credential verification,
 * account lockout, scope validation, and token issuance/rotation — lives in
 * {@link TokenService} so that it can be unit-tested without a Spring context.
 * Rejected grants are reported as {@code TokenGrantException} subclasses and mapped
 * to HTTP error responses by {@code TokenExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/oauth2/user")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    /**
     * Issues (or refreshes) an OAuth2 access/refresh token pair.
     *
     * @param grantType    {@code "password"} or {@code "refresh_token"}; any other
     *                     value results in an {@code unsupported_grant_type} error
     * @param username     resource-owner email; required for the password grant
     * @param password     resource-owner password; required for the password grant
     * @param refreshToken the refresh token to redeem; required for the refresh-token grant
     * @param scope        space-separated scopes requested by the client (password grant only);
     *                     defaults to the user's primary role scope when omitted
     * @return the issued token pair, serialized as the standard OAuth2 token response
     */
    @PostMapping(value = "/token", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    public ResponseEntity<TokenGrantResultDto> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope", required = false, defaultValue = "") String scope) {

        log.info("POST /oauth2/user/token — grant_type={}", grantType);

        TokenGrantResultDto result = tokenService.grantToken(grantType, username, password, refreshToken, scope);

        log.info("POST /oauth2/user/token — grant_type={} succeeded", grantType);

        return ResponseEntity.ok(result);
    }
}


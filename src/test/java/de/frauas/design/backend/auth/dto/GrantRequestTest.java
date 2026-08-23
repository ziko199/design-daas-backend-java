package de.frauas.design.backend.auth.dto;

import de.frauas.design.backend.auth.dto.GrantRequest.PasswordGrantRequest;
import de.frauas.design.backend.auth.dto.GrantRequest.RefreshGrantRequest;
import de.frauas.design.backend.auth.exception.UnsupportedGrantTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GrantRequest")
class GrantRequestTest {

    @Test
    @DisplayName("maps grant_type=password to a PasswordGrantRequest carrying the given fields")
    void password_mapsToPasswordGrantRequest() {
        GrantRequest request = GrantRequest.of("password", "user@example.com", "secret", null, "user");

        assertThat(request).isInstanceOf(PasswordGrantRequest.class);
        PasswordGrantRequest passwordRequest = (PasswordGrantRequest) request;
        assertThat(passwordRequest.username()).isEqualTo("user@example.com");
        assertThat(passwordRequest.password()).isEqualTo("secret");
        assertThat(passwordRequest.scope()).isEqualTo("user");
    }

    @Test
    @DisplayName("maps grant_type=refresh_token to a RefreshGrantRequest carrying the token")
    void refreshToken_mapsToRefreshGrantRequest() {
        GrantRequest request = GrantRequest.of("refresh_token", null, null, "the-refresh-token", null);

        assertThat(request).isInstanceOf(RefreshGrantRequest.class);
        assertThat(((RefreshGrantRequest) request).refreshToken()).isEqualTo("the-refresh-token");
    }

    @Test
    @DisplayName("throws UnsupportedGrantTypeException(400) for any other grant_type")
    void unknownGrantType_throwsUnsupportedGrantType() {
        assertThatThrownBy(() -> GrantRequest.of("client_credentials", null, null, null, null))
                .isInstanceOf(UnsupportedGrantTypeException.class)
                .extracting(ex -> ((UnsupportedGrantTypeException) ex).getStatus())
                .isEqualTo(400);
    }
}

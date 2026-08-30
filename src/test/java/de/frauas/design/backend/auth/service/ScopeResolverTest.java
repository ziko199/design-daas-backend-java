package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.exception.InvalidScopeException;
import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ScopeResolver")
class ScopeResolverTest {

    private final ScopeResolver scopeResolver = new ScopeResolver();

    @Test
    @DisplayName("resolveAuthorizedScope defaults blank input to the user's role")
    void resolveAuthorizedScope_blankInput_defaultsToRole() {
        assertThat(scopeResolver.resolveAuthorizedScope(user(), "   ")).isEqualTo("user");
    }

    @Test
    @DisplayName("resolveAuthorizedScope accepts the exact role scope")
    void resolveAuthorizedScope_matchingScope_returnsRole() {
        assertThat(scopeResolver.resolveAuthorizedScope(admin(), "admin")).isEqualTo("admin");
    }

    @Test
    @DisplayName("resolveAuthorizedScope rejects scope escalation")
    void resolveAuthorizedScope_scopeEscalation_throws() {
        assertThatThrownBy(() -> scopeResolver.resolveAuthorizedScope(user(), "admin"))
                .isInstanceOf(InvalidScopeException.class);
    }

    @Test
    @DisplayName("parsePersistedScope trims stored scopes")
    void parsePersistedScope_trimsStoredScope() {
        assertThat(scopeResolver.parsePersistedScope(" user ")).isEqualTo("user");
    }

    @Test
    @DisplayName("parsePersistedScope defaults blank data to the least-privileged scope")
    void parsePersistedScope_blankScope_defaultsToUser() {
        assertThat(scopeResolver.parsePersistedScope("   ")).isEqualTo("user");
        assertThat(scopeResolver.parsePersistedScope(null)).isEqualTo("user");
    }

    private User user() {
        User user = new User();
        user.setEmail("user@example.com");
        return user;
    }

    private Admin admin() {
        Admin admin = new Admin();
        admin.setEmail("admin@example.com");
        return admin;
    }
}

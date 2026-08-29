package de.frauas.design.backend.permission.controller;

import de.frauas.design.backend.auth.service.JwtUserResolver;
import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionController")
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    @Mock
    private JwtUserResolver jwtUserResolver;

    @Mock
    private Jwt authJwt;

    @InjectMocks
    private PermissionController permissionController;

    @Test
    @DisplayName("throws IllegalArgumentException when request body is null")
    void permissionsInfo_nullBody_throwsBadRequest() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> permissionController.permissionsInfo(null, authJwt));

        verifyNoInteractions(permissionService, jwtUserResolver);
    }

    @Test
    @DisplayName("prefers body token over authenticated JWT")
    void permissionsInfo_bodyTokenTakesPrecedence() {
        when(jwtUserResolver.resolveFromRawToken("raw-token")).thenReturn(7);
        when(permissionService.checkPermission(7, "test_function")).thenReturn(PermissionResultDto.allow(7, "Alice"));

        var response = permissionController.permissionsInfo(
                Map.of("function_name", "test_function", "token", "raw-token"), authJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResult()).isEqualTo("allow");
        verify(jwtUserResolver).resolveFromRawToken("raw-token");
        verify(jwtUserResolver, never()).resolveFromAuthenticatedJwt(authJwt);
    }

    @Test
    @DisplayName("returns deny placeholder when no user can be resolved")
    void permissionsInfo_unresolvableUser_returnsDenyPlaceholder() {
        when(jwtUserResolver.resolveFromAuthenticatedJwt(authJwt)).thenReturn(null);

        var response = permissionController.permissionsInfo(Map.of("function_name", "test_function"), authJwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getResult()).isEqualTo("deny");
        assertThat(response.getBody().getUser()).containsEntry("id", 0).containsEntry("name", "unknown");
        verifyNoInteractions(permissionService);
    }
}

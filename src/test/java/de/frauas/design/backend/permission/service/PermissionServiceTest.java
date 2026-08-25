package de.frauas.design.backend.permission.service;

import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.model.Endpoint;
import de.frauas.design.backend.permission.model.EndpointGroup;
import de.frauas.design.backend.permission.model.EndpointGroupUserAccess;
import de.frauas.design.backend.permission.model.EndpointGroupUserGroupAccess;
import de.frauas.design.backend.permission.model.EndpointUserAccess;
import de.frauas.design.backend.permission.model.EndpointUserGroupAccess;
import de.frauas.design.backend.permission.repository.EndpointGroupUserAccessRepository;
import de.frauas.design.backend.permission.repository.EndpointGroupUserGroupAccessRepository;
import de.frauas.design.backend.permission.repository.EndpointUserAccessRepository;
import de.frauas.design.backend.permission.repository.EndpointUserGroupAccessRepository;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PermissionService covering all 4 resolution steps
 * plus the default-allow fallback.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService")
class PermissionServiceTest {

    private static final int USER_ID = 1;
    private static final String FUNCTION = "get_desktops";

    @Mock
    EndpointUserAccessRepository endpointUserAccessRepo;

    @Mock
    EndpointUserGroupAccessRepository endpointUserGroupAccessRepo;

    @Mock
    EndpointGroupUserAccessRepository endpointGroupUserAccessRepo;

    @Mock
    EndpointGroupUserGroupAccessRepository endpointGroupUserGroupAccessRepo;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    PermissionService permissionService;

    @BeforeEach
    void stubUserLookup() {
        User u = new User();
        u.setId(USER_ID);
        u.setName("Alice");
        u.setEmail("alice@example.com");
        u.setPassword("hashed");
        u.setEnabled(true);
        // lenient: some tests (unknownUser_*) call with a different userId so this stub is unused
        lenient().when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
    }

    // -------------------------------------------------------------------------
    // Step 1 – direct user → endpoint rule
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("uses 'unknown' as user name when user ID not found")
    void unknownUser_usesUnknownName() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(999, FUNCTION))
                .thenReturn(Optional.empty());
        when(userRepository.findGroupIdsByUserId(999)).thenReturn(List.of());
        when(endpointGroupUserAccessRepo.findByUserId(999)).thenReturn(List.of());

        PermissionResultDto result = permissionService.checkPermission(999, FUNCTION);

        assertThat(result.getUser()).containsEntry("name", "unknown");
    }

    // -------------------------------------------------------------------------
    // Step 2 – user group → endpoint rule
    // -------------------------------------------------------------------------

    private EndpointUserAccess makeEndpointUserAccess(boolean allow) {
        Endpoint ep = new Endpoint();
        ep.setFunctionName(FUNCTION);

        EndpointUserAccess r = new EndpointUserAccess();
        r.setUserId(USER_ID);
        r.setEndpoint(ep);
        r.setAllowAccess(allow);
        return r;
    }

    // -------------------------------------------------------------------------
    // Step 3 – user → endpoint group rule
    // -------------------------------------------------------------------------

    private EndpointUserGroupAccess makeEndpointUserGroupAccess(boolean allow) {
        Endpoint ep = new Endpoint();
        ep.setFunctionName(FUNCTION);

        EndpointUserGroupAccess r = new EndpointUserGroupAccess();
        r.setUserGroupId(10);
        r.setEndpoint(ep);
        r.setAllowAccess(allow);
        return r;
    }

    // -------------------------------------------------------------------------
    // Step 4 – user group → endpoint group rule
    // -------------------------------------------------------------------------

    private EndpointGroupUserAccess makeEndpointGroupUserAccess(String functionName, boolean allow) {
        Endpoint ep = new Endpoint();
        ep.setFunctionName(functionName);

        EndpointGroup eg = new EndpointGroup();
        eg.setId(100);
        eg.setName("TestGroup");
        eg.getEndpoints().add(ep);

        EndpointGroupUserAccess r = new EndpointGroupUserAccess();
        r.setUserId(USER_ID);
        r.setEndpointGroup(eg);
        r.setAllowAccess(allow);
        return r;
    }

    // -------------------------------------------------------------------------
    // Default fallback
    // -------------------------------------------------------------------------

    private EndpointGroupUserGroupAccess makeEndpointGroupUserGroupAccess(String functionName, boolean allow) {
        Endpoint ep = new Endpoint();
        ep.setFunctionName(functionName);

        EndpointGroup eg = new EndpointGroup();
        eg.setId(200);
        eg.setName("GroupEG");
        eg.getEndpoints().add(ep);

        EndpointGroupUserGroupAccess r = new EndpointGroupUserGroupAccess();
        r.setUserGroupId(10);
        r.setEndpointGroup(eg);
        r.setAllowAccess(allow);
        return r;
    }

    // -------------------------------------------------------------------------
    // Unknown user
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Step 1 – direct endpoint user access")
    class Step1 {

        @Test
        @DisplayName("returns ALLOW when direct allow rule found")
        void step1_directAllow_returnsAllow() {
            EndpointUserAccess rule = makeEndpointUserAccess(true);
            when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(USER_ID, FUNCTION))
                    .thenReturn(Optional.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("allow");
            assertThat(result.getUser()).containsEntry("id", USER_ID);
            // Step 2–4 must never be consulted
            verifyNoInteractions(
                    endpointUserGroupAccessRepo, endpointGroupUserAccessRepo, endpointGroupUserGroupAccessRepo);
        }

        @Test
        @DisplayName("returns DENY when direct deny rule found")
        void step1_directDeny_returnsDeny() {
            EndpointUserAccess rule = makeEndpointUserAccess(false);
            when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(USER_ID, FUNCTION))
                    .thenReturn(Optional.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("deny");
        }
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Step 2 – user group endpoint access")
    class Step2 {

        @BeforeEach
        void noDirectRule() {
            when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(USER_ID, FUNCTION))
                    .thenReturn(Optional.empty());
            when(userRepository.findGroupIdsByUserId(USER_ID)).thenReturn(List.of(10, 20));
        }

        @Test
        @DisplayName("returns ALLOW via group rule")
        void step2_groupAllow_returnsAllow() {
            EndpointUserGroupAccess rule = makeEndpointUserGroupAccess(true);
            when(endpointUserGroupAccessRepo.findFirstByUserGroupIdInAndEndpoint_FunctionName(
                            List.of(10, 20), FUNCTION))
                    .thenReturn(Optional.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("allow");
            verifyNoInteractions(endpointGroupUserAccessRepo, endpointGroupUserGroupAccessRepo);
        }

        @Test
        @DisplayName("returns DENY via group rule")
        void step2_groupDeny_returnsDeny() {
            EndpointUserGroupAccess rule = makeEndpointUserGroupAccess(false);
            when(endpointUserGroupAccessRepo.findFirstByUserGroupIdInAndEndpoint_FunctionName(
                            List.of(10, 20), FUNCTION))
                    .thenReturn(Optional.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("deny");
        }
    }

    @Nested
    @DisplayName("Step 3 – user endpoint-group access")
    class Step3 {

        @BeforeEach
        void noStep1or2() {
            when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(USER_ID, FUNCTION))
                    .thenReturn(Optional.empty());
            when(userRepository.findGroupIdsByUserId(USER_ID)).thenReturn(List.of());
        }

        @Test
        @DisplayName("returns ALLOW when function is in user's endpoint group (allow rule)")
        void step3_endpointGroupAllow_returnsAllow() {
            EndpointGroupUserAccess rule = makeEndpointGroupUserAccess(FUNCTION, true);
            when(endpointGroupUserAccessRepo.findByUserId(USER_ID)).thenReturn(List.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("allow");
            verifyNoInteractions(endpointGroupUserGroupAccessRepo);
        }

        @Test
        @DisplayName("returns DENY when function is in user's endpoint group (deny rule)")
        void step3_endpointGroupDeny_returnsDeny() {
            EndpointGroupUserAccess rule = makeEndpointGroupUserAccess(FUNCTION, false);
            when(endpointGroupUserAccessRepo.findByUserId(USER_ID)).thenReturn(List.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("deny");
        }

        @Test
        @DisplayName("falls through to default when function NOT in any endpoint group")
        void step3_functionNotInGroup_fallsThrough() {
            EndpointGroupUserAccess rule = makeEndpointGroupUserAccess("other_function", true);
            when(endpointGroupUserAccessRepo.findByUserId(USER_ID)).thenReturn(List.of(rule));
            // groupIds is empty (from @BeforeEach) so step 4 is not reached at all

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult())
                    .isEqualTo("deny"); // default: deny (matches PHP PermissionCalculationService)
        }
    }

    @Nested
    @DisplayName("Step 4 – user-group endpoint-group access")
    class Step4 {

        @BeforeEach
        void noStep1to3() {
            when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(USER_ID, FUNCTION))
                    .thenReturn(Optional.empty());
            when(userRepository.findGroupIdsByUserId(USER_ID)).thenReturn(List.of(10));
            when(endpointUserGroupAccessRepo.findFirstByUserGroupIdInAndEndpoint_FunctionName(any(), any()))
                    .thenReturn(Optional.empty());
            when(endpointGroupUserAccessRepo.findByUserId(USER_ID)).thenReturn(List.of());
        }

        @Test
        @DisplayName("returns ALLOW via user-group endpoint-group rule")
        void step4_groupEndpointGroupAllow_returnsAllow() {
            EndpointGroupUserGroupAccess rule = makeEndpointGroupUserGroupAccess(FUNCTION, true);
            when(endpointGroupUserGroupAccessRepo.findByUserGroupIdIn(List.of(10)))
                    .thenReturn(List.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("allow");
        }

        @Test
        @DisplayName("returns DENY via user-group endpoint-group rule")
        void step4_groupEndpointGroupDeny_returnsDeny() {
            EndpointGroupUserGroupAccess rule = makeEndpointGroupUserGroupAccess(FUNCTION, false);
            when(endpointGroupUserGroupAccessRepo.findByUserGroupIdIn(List.of(10)))
                    .thenReturn(List.of(rule));

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("deny");
        }
    }

    @Nested
    @DisplayName("Default fallback")
    class DefaultFallback {

        @Test
        @DisplayName("returns DENY when no rule matches (matches PHP PermissionCalculationService)")
        void noRuleMatches_returnsDeny() {
            when(endpointUserAccessRepo.findByUserIdAndEndpoint_FunctionName(USER_ID, FUNCTION))
                    .thenReturn(Optional.empty());
            when(userRepository.findGroupIdsByUserId(USER_ID)).thenReturn(List.of());
            when(endpointGroupUserAccessRepo.findByUserId(USER_ID)).thenReturn(List.of());

            PermissionResultDto result = permissionService.checkPermission(USER_ID, FUNCTION);

            assertThat(result.getResult()).isEqualTo("deny");
        }
    }
}

package de.frauas.design.backend.permission.service;

import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.repository.EndpointGroupUserAccessRepository;
import de.frauas.design.backend.permission.repository.EndpointGroupUserGroupAccessRepository;
import de.frauas.design.backend.permission.repository.EndpointUserAccessRepository;
import de.frauas.design.backend.permission.repository.EndpointUserGroupAccessRepository;
import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Resolves whether a given user is allowed to invoke a given design-daas function.
 *
 * <p>Resolution follows the 4-step cascade defined by the design-daas contract (ported from
 * the legacy PHP {@code PermissionCalculationService}), evaluated in order of specificity —
 * the first matching rule wins:
 * <ol>
 *   <li>direct user → endpoint rule</li>
 *   <li>user's group → endpoint rule</li>
 *   <li>user → endpoint-group rule</li>
 *   <li>user's group → endpoint-group rule</li>
 * </ol>
 *
 * <p>Default: <b>DENY</b> — if no rule matches at any step, access is denied.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String UNKNOWN_USER_NAME = "unknown";
    private static final int UNRESOLVED_USER_ID = 0;

    private final EndpointUserAccessRepository endpointUserAccessRepo;
    private final EndpointUserGroupAccessRepository endpointUserGroupAccessRepo;
    private final EndpointGroupUserAccessRepository endpointGroupUserAccessRepo;
    private final EndpointGroupUserGroupAccessRepository endpointGroupUserGroupAccessRepo;
    private final UserRepository userRepository;

    /**
     * Checks whether {@code userId} is allowed to invoke {@code functionName}, applying the
     * 4-step cascade described in the class Javadoc.
     *
     * @param userId       the ID of the user to check; if unknown or {@code null}, no rule can
     *                     match and the result defaults to deny
     * @param functionName the design-daas function name being invoked; if {@code null} or
     *                     blank, the result defaults to deny without querying any rule
     * @return the permission result ({@code allow}/{@code deny}) together with basic user info
     */
    @Transactional(readOnly = true)
    public PermissionResultDto checkPermission(Integer userId, String functionName) {
        log.debug("checkPermission — userId={} functionName={}", userId, functionName);

        if (userId == null || functionName == null || functionName.isBlank()) {
            log.warn(
                    "checkPermission — invalid input (userId={}, functionName={}), defaulting to deny",
                    userId,
                    functionName);
            // PermissionResultDto backs its "user" map with Map.of(), which rejects nulls,
            // so a null userId is normalized to a placeholder id instead of being passed through.
            return PermissionResultDto.deny(userId != null ? userId : UNRESOLVED_USER_ID, UNKNOWN_USER_NAME);
        }

        String userName = userRepository.findById(userId).map(BaseUser::getName).orElse(UNKNOWN_USER_NAME);
        List<Integer> groupIds = userRepository.findGroupIdsByUserId(userId);

        Optional<Boolean> allow = checkDirectUserRule(userId, functionName)
                .or(() -> checkUserGroupRule(groupIds, functionName))
                .or(() -> checkUserEndpointGroupRule(userId, functionName))
                .or(() -> checkUserGroupEndpointGroupRule(groupIds, functionName));

        if (allow.isEmpty()) {
            log.info("checkPermission — no rule matched, defaulting to deny userId={} fn={}", userId, functionName);
            return PermissionResultDto.deny(userId, userName);
        }

        log.info("checkPermission — userId={} fn={} result={}", userId, functionName, allow.get() ? "allow" : "deny");
        return toResult(allow.get(), userId, userName);
    }

    /** Step 1: direct user → endpoint rule. */
    private Optional<Boolean> checkDirectUserRule(Integer userId, String functionName) {
        return endpointUserAccessRepo
                .findByUserIdAndEndpoint_FunctionName(userId, functionName)
                .map(rule -> {
                    log.debug("checkPermission — step1 (direct user-endpoint) matched userId={}", userId);
                    return rule.isAllowAccess();
                });
    }

    /** Step 2: user's group membership → endpoint rule. */
    private Optional<Boolean> checkUserGroupRule(List<Integer> groupIds, String functionName) {
        if (groupIds.isEmpty()) {
            return Optional.empty();
        }
        return endpointUserGroupAccessRepo
                .findFirstByUserGroupIdInAndEndpoint_FunctionName(groupIds, functionName)
                .map(rule -> {
                    log.debug("checkPermission — step2 (group-endpoint) matched groupIds={}", groupIds);
                    return rule.isAllowAccess();
                });
    }

    /** Step 3: user → endpoint-group rule (function must belong to that endpoint group). */
    private Optional<Boolean> checkUserEndpointGroupRule(Integer userId, String functionName) {
        return endpointGroupUserAccessRepo
                .findFirstByUserIdAndEndpointGroup_Endpoints_FunctionName(userId, functionName)
                .map(rule -> {
                    log.debug("checkPermission — step3 (user-endpointGroup) matched userId={}", userId);
                    return rule.isAllowAccess();
                });
    }

    /** Step 4: user's group → endpoint-group rule (function must belong to that endpoint group). */
    private Optional<Boolean> checkUserGroupEndpointGroupRule(List<Integer> groupIds, String functionName) {
        if (groupIds.isEmpty()) {
            return Optional.empty();
        }
        return endpointGroupUserGroupAccessRepo
                .findFirstByUserGroupIdInAndEndpointGroup_Endpoints_FunctionName(groupIds, functionName)
                .map(rule -> {
                    log.debug("checkPermission — step4 (group-endpointGroup) matched groupIds={}", groupIds);
                    return rule.isAllowAccess();
                });
    }

    private PermissionResultDto toResult(boolean allow, Integer userId, String userName) {
        return allow ? PermissionResultDto.allow(userId, userName) : PermissionResultDto.deny(userId, userName);
    }
}

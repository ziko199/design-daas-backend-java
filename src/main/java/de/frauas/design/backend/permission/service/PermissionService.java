package de.frauas.design.backend.permission.service;

import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.repository.*;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final EndpointUserAccessRepository endpointUserAccessRepo;
    private final EndpointUserGroupAccessRepository endpointUserGroupAccessRepo;
    private final EndpointGroupUserAccessRepository endpointGroupUserAccessRepo;
    private final EndpointGroupUserGroupAccessRepository endpointGroupUserGroupAccessRepo;
    private final UserRepository userRepository;

    /**
     * 4-step permission lookup as defined by the design-daas contract.
     * Default (implicit): DENY — if no rule matches, access is denied (doc Section 11).
     */
    @Transactional(readOnly = true)
    public PermissionResultDto checkPermission(Integer userId, String functionName) {
        log.debug("checkPermission — userId={} functionName={}", userId, functionName);
        String userName = userRepository.findById(userId)
            .map(u -> u.getName())
            .orElse("unknown");

        // Step 1: Direct user → endpoint rule
        var direct = endpointUserAccessRepo
            .findByUserIdAndEndpoint_FunctionName(userId, functionName);
        if (direct.isPresent()) {
            boolean allow = direct.get().isAllowAccess();
            log.debug("checkPermission — step1 (direct user-endpoint) userId={} fn={} result={}", userId, functionName, allow ? "allow" : "deny");
            return result(allow, userId, userName);
        }

        // Step 2: User's group membership → endpoint rule
        List<Integer> groupIds = userRepository.findGroupIdsByUserId(userId);
        if (!groupIds.isEmpty()) {
            var groupRule = endpointUserGroupAccessRepo
                .findFirstByUserGroupIdInAndEndpoint_FunctionName(groupIds, functionName);
            if (groupRule.isPresent()) {
                boolean allow = groupRule.get().isAllowAccess();
                log.debug("checkPermission — step2 (group-endpoint) userId={} fn={} result={}", userId, functionName, allow ? "allow" : "deny");
                return result(allow, userId, userName);
            }
        }

        // Step 3: User → endpoint-group rule (check if function is in that group)
        var userEgRules = endpointGroupUserAccessRepo.findByUserId(userId);
        for (var rule : userEgRules) {
            boolean matches = rule.getEndpointGroup().getEndpoints().stream()
                .anyMatch(e -> e.getFunctionName().equals(functionName));
            if (matches) {
                boolean allow = rule.isAllowAccess();
                log.debug("checkPermission — step3 (user-endpointGroup) userId={} fn={} result={}", userId, functionName, allow ? "allow" : "deny");
                return result(allow, userId, userName);
            }
        }

        // Step 4: User's groups → endpoint-group rule
        if (!groupIds.isEmpty()) {
            var groupEgRules = endpointGroupUserGroupAccessRepo.findByUserGroupIdIn(groupIds);
            for (var rule : groupEgRules) {
                boolean matches = rule.getEndpointGroup().getEndpoints().stream()
                    .anyMatch(e -> e.getFunctionName().equals(functionName));
                if (matches) {
                    boolean allow = rule.isAllowAccess();
                    log.debug("checkPermission — step4 (group-endpointGroup) userId={} fn={} result={}", userId, functionName, allow ? "allow" : "deny");
                    return result(allow, userId, userName);
                }
            }
        }

        log.debug("checkPermission — no rule matched, defaulting to deny userId={} fn={}", userId, functionName);
        return PermissionResultDto.deny(userId, userName);
    }

    private PermissionResultDto result(boolean allow, Integer userId, String userName) {
        return allow
            ? PermissionResultDto.allow(userId, userName)
            : PermissionResultDto.deny(userId, userName);
    }
}

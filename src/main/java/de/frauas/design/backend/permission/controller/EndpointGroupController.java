package de.frauas.design.backend.permission.controller;

import de.frauas.design.backend.permission.service.EndpointGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SCOPE_admin')")
public class EndpointGroupController {

    private final EndpointGroupService endpointGroupService;

    @PutMapping("/endpoint_group/{endpointGroupName}/{userGroupId}/{access}")
    public ResponseEntity<Void> setEndpointGroupAccess(
            @PathVariable String endpointGroupName,
            @PathVariable Integer userGroupId,
            @PathVariable String access) {
        log.info("PUT /endpoint_group/{}/{}/{} — setting access rule", endpointGroupName, userGroupId, access);
        if (!access.equals("allow") && !access.equals("deny")) {
            log.warn("PUT /endpoint_group — invalid access value '{}' for group={} userGroup={}",
                    access, endpointGroupName, userGroupId);
            return ResponseEntity.badRequest().build();
        }
        endpointGroupService.associateUserGroup(endpointGroupName, userGroupId, access);
        log.info("PUT /endpoint_group/{}/{}/{} — access rule saved", endpointGroupName, userGroupId, access);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/endpoint_group/{endpointGroupId}/{userGroupId}")
    public ResponseEntity<Void> deleteEndpointGroupAccess(
            @PathVariable String endpointGroupId,
            @PathVariable Integer userGroupId) {
        log.info("DELETE /endpoint_group/{}/{} — removing access rule", endpointGroupId, userGroupId);
        endpointGroupService.deleteEndpointGroupUserGroupAccess(endpointGroupId, userGroupId);
        log.info("DELETE /endpoint_group/{}/{} — access rule removed", endpointGroupId, userGroupId);
        return ResponseEntity.ok().build();
    }
}

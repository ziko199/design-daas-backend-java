package de.frauas.design.backend.permission.service;

import de.frauas.design.backend.permission.model.EndpointGroupUserGroupAccess;
import de.frauas.design.backend.permission.repository.EndpointGroupRepository;
import de.frauas.design.backend.permission.repository.EndpointGroupUserGroupAccessRepository;
import de.frauas.design.backend.user.repository.UserGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Manages associations between endpoint groups and user groups.
 * Mirrors PHP EndpointGroupService.
 */
@Service
@RequiredArgsConstructor
public class EndpointGroupService {

    private final EndpointGroupRepository endpointGroupRepository;
    private final UserGroupRepository userGroupRepository;
    private final EndpointGroupUserGroupAccessRepository accessRepository;

    /**
     * Creates or updates the access rule for a user group on an endpoint group.
     * Mirrors PHP EndpointGroupService::associateUserGroup().
     *
     * @param endpointGroupName the unique name of the endpoint group
     * @param userGroupId       the ID of the user group
     * @param access            "allow" or "deny"
     */
    @Transactional
    public void associateUserGroup(String endpointGroupName, Integer userGroupId, String access) {
        var endpointGroup = endpointGroupRepository.findByName(endpointGroupName)
            .orElseThrow(() -> new NoSuchElementException("Endpoint group not found: " + endpointGroupName));
        userGroupRepository.findById(userGroupId)
            .orElseThrow(() -> new NoSuchElementException("User group not found: " + userGroupId));

        boolean allowAccess = "allow".equalsIgnoreCase(access);

        var existing = accessRepository
            .findByUserGroupIdAndEndpointGroup_Id(userGroupId, endpointGroup.getId());
        if (existing.isPresent()) {
            existing.get().setAllowAccess(allowAccess);
            accessRepository.save(existing.get());
        } else {
            EndpointGroupUserGroupAccess record = new EndpointGroupUserGroupAccess();
            record.setUserGroupId(userGroupId);
            record.setEndpointGroup(endpointGroup);
            record.setAllowAccess(allowAccess);
            accessRepository.save(record);
        }
    }

    /**
     * Removes the access rule for a user group on an endpoint group.
     * Mirrors PHP EndpointGroupService::deleteEndpointGroupUserGroupAccess().
     *
     * @param endpointGroupName the unique name of the endpoint group
     * @param userGroupId       the ID of the user group
     */
    @Transactional
    public void deleteEndpointGroupUserGroupAccess(String endpointGroupName, Integer userGroupId) {
        var endpointGroup = endpointGroupRepository.findByName(endpointGroupName)
            .orElseThrow(() -> new NoSuchElementException("Endpoint group not found: " + endpointGroupName));
        userGroupRepository.findById(userGroupId)
            .orElseThrow(() -> new NoSuchElementException("User group not found: " + userGroupId));

        var existing = accessRepository
            .findByUserGroupIdAndEndpointGroup_Id(userGroupId, endpointGroup.getId());
        if (existing.isEmpty()) {
            throw new NoSuchElementException(
                "No access record found for user group " + userGroupId +
                " on endpoint group " + endpointGroupName);
        }
        accessRepository.delete(existing.get());
    }
}

package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointGroupUserGroupAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndpointGroupUserGroupAccessRepository
        extends JpaRepository<EndpointGroupUserGroupAccess, Integer> {

    List<EndpointGroupUserGroupAccess> findByUserGroupIdIn(List<Integer> userGroupIds);

    /** Used by EndpointGroupService to check for existing access records. */
    Optional<EndpointGroupUserGroupAccess> findByUserGroupIdAndEndpointGroup_Id(
            Integer userGroupId, Integer endpointGroupId);

    /** Used by EndpointGroupService to delete access records. */
    void deleteByUserGroupIdAndEndpointGroup_Id(Integer userGroupId, Integer endpointGroupId);
}

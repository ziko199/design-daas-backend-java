package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointGroupUserGroupAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndpointGroupUserGroupAccessRepository extends JpaRepository<EndpointGroupUserGroupAccess, Integer> {

    /**
     * Finds a user-group's endpoint-group access rule whose endpoint group contains an endpoint
     * with the given function name. Filtering is done in the query so callers don't need to
     * load every rule/endpoint-group for the user's groups and filter in memory.
     */
    Optional<EndpointGroupUserGroupAccess> findFirstByUserGroupIdInAndEndpointGroup_Endpoints_FunctionName(
            List<Integer> userGroupIds, String functionName);
}

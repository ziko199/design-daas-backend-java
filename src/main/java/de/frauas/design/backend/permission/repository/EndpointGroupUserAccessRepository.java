package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointGroupUserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EndpointGroupUserAccessRepository extends JpaRepository<EndpointGroupUserAccess, Integer> {

    /**
     * Finds a user's endpoint-group access rule whose endpoint group contains an endpoint
     * with the given function name. Filtering is done in the query so callers don't need to
     * load every rule/endpoint-group for the user and filter in memory.
     */
    Optional<EndpointGroupUserAccess> findFirstByUserIdAndEndpointGroup_Endpoints_FunctionName(
            Integer userId, String functionName);
}

package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointUserGroupAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EndpointUserGroupAccessRepository extends JpaRepository<EndpointUserGroupAccess, Integer> {

    Optional<EndpointUserGroupAccess> findFirstByUserGroupIdInAndEndpoint_FunctionName(
            List<Integer> userGroupIds, String functionName);
}

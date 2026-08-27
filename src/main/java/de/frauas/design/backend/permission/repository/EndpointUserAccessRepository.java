package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointUserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for direct per-user, per-endpoint permission rules (resolution step 1).
 */
@Repository
public interface EndpointUserAccessRepository extends JpaRepository<EndpointUserAccess, Integer> {
    Optional<EndpointUserAccess> findByUserIdAndEndpoint_FunctionName(Integer userId, String functionName);
}

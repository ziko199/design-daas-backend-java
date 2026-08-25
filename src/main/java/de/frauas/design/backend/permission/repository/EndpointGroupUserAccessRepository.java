package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointGroupUserAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EndpointGroupUserAccessRepository extends JpaRepository<EndpointGroupUserAccess, Integer> {

    List<EndpointGroupUserAccess> findByUserId(Integer userId);
}

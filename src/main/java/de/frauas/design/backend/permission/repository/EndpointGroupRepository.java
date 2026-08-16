package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.EndpointGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EndpointGroupRepository extends JpaRepository<EndpointGroup, Integer> {

    /** Find endpoint group by its unique name (mirrors PHP EndpointGroupRepository::findByUniqueEndpointGroupName). */
    Optional<EndpointGroup> findByName(String name);
}

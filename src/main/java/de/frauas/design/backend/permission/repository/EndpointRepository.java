package de.frauas.design.backend.permission.repository;

import de.frauas.design.backend.permission.model.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Integer> {
    Optional<Endpoint> findByFunctionName(String functionName);
}

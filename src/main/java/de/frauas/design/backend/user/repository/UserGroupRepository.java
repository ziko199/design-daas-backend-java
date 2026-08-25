package de.frauas.design.backend.user.repository;

import de.frauas.design.backend.user.model.UserGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Data-access for {@link UserGroup} entities. */
@Repository
public interface UserGroupRepository extends JpaRepository<UserGroup, Integer> {
    Optional<UserGroup> findByName(String name);
}

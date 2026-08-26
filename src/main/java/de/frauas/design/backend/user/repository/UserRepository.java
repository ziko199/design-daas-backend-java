package de.frauas.design.backend.user.repository;

import de.frauas.design.backend.user.model.Admin;
import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Data-access for {@link BaseUser} and its subtypes ({@link User}, {@link Admin}).
 * Uses single-table inheritance, so most queries filter by concrete
 * subtype via JPQL.
 */
@Repository
public interface UserRepository extends JpaRepository<BaseUser, Integer> {

    Optional<BaseUser> findByEmail(String email);

    /** Returns ALL regular users ordered by id — service-layer pagination uses this. */
    @Query("SELECT u FROM User u ORDER BY u.id")
    List<User> findAllUsers();

    @Query("SELECT a FROM Admin a ORDER BY a.id")
    List<Admin> findAllAdmins();

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findUserById(@Param("id") Integer id);

    @Query("SELECT a FROM Admin a WHERE a.id = :id")
    Optional<Admin> findAdminById(@Param("id") Integer id);

    /** Used by PermissionService to fetch the user's group IDs. */
    @Query("SELECT ug.id FROM User u JOIN u.groups ug WHERE u.id = :userId")
    List<Integer> findGroupIdsByUserId(@Param("userId") Integer userId);
}

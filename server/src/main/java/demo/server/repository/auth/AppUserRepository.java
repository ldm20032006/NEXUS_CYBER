package demo.server.repository.auth;

import demo.server.entity.auth.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "branch"})
    Optional<AppUser> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "branch"})
    Optional<AppUser> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "branch"})
    @Query("select u from AppUser u where u.email = :identifier or u.phone = :identifier")
    Optional<AppUser> findByEmailOrPhone(String identifier);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "branch"})
    List<AppUser> findAllByBranch_Id(UUID branchId);
}

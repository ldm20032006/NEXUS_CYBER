package demo.server.repository.auth;

import demo.server.entity.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByFamilyId(UUID familyId);

    List<RefreshToken> findAllByUser_IdAndRevokedAtIsNull(UUID userId);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before or (t.revokedAt is not null and t.revokedAt < :before)")
    int deleteExpiredOrRevokedBefore(Instant before);
}

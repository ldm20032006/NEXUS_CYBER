package demo.server.repository.auth;

import demo.server.entity.auth.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :before or (t.usedAt is not null and t.usedAt < :before) or (t.revokedAt is not null and t.revokedAt < :before)")
    int deleteExpiredOrUsedBefore(Instant before);
}

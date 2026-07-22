package demo.server.repository.session;

import demo.server.common.enums.QrLoginSessionStatus;
import demo.server.entity.session.QrLoginSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QrLoginSessionRepository extends JpaRepository<QrLoginSession, UUID> {
    Optional<QrLoginSession> findByNonce(String nonce);

    Optional<QrLoginSession> findByIdempotencyKey(String idempotencyKey);

    @Query("select q.id from QrLoginSession q where q.status = :status and q.expiresAt <= :now order by q.expiresAt asc")
    List<UUID> findExpiredIds(QrLoginSessionStatus status, Instant now, Pageable pageable);

    @Modifying
    @Query("update QrLoginSession q set q.status = :next where q.id in :ids and q.status = :expected")
    int updateStatusByIds(List<UUID> ids, QrLoginSessionStatus expected, QrLoginSessionStatus next);
}

package demo.server.repository.session;

import demo.server.entity.session.PlaySession;
import demo.server.common.enums.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaySessionRepository extends JpaRepository<PlaySession, UUID>, JpaSpecificationExecutor<PlaySession> {
    Optional<PlaySession> findFirstByUser_IdAndStatusOrderByStartedAtDesc(UUID userId, SessionStatus status);

    Optional<PlaySession> findFirstByStation_IdAndStatusOrderByStartedAtDesc(UUID stationId, SessionStatus status);

    List<PlaySession> findByUser_IdOrderByStartedAtDesc(UUID userId);

    List<PlaySession> findByStation_IdOrderByStartedAtDesc(UUID stationId);

    Optional<PlaySession> findByIdempotencyKey(String idempotencyKey);

    boolean existsByUser_IdAndStatus(UUID userId, SessionStatus status);

    boolean existsByStation_IdAndStatus(UUID stationId, SessionStatus status);

    @Query("select s from PlaySession s where s.status = :status and s.startedAt <= :before order by s.startedAt asc")
    List<PlaySession> findSessionsNeedingEndingWarning(SessionStatus status, Instant before, Pageable pageable);
}

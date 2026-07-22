package demo.server.common.resilience;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class InMemoryIdempotencyService implements IdempotencyService {

    private final ConcurrentHashMap<String, MutableIdempotencyRecord> records = new ConcurrentHashMap<>();

    @Override
    public IdempotencyDecision begin(String key, String fingerprint, Duration ttl) {
        Instant now = Instant.now();
        AtomicBoolean created = new AtomicBoolean(false);
        MutableIdempotencyRecord record = records.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt.isBefore(now)) {
                created.set(true);
                return new MutableIdempotencyRecord(key, fingerprint, IdempotencyStatus.STARTED, null, now, now, now.plus(ttl));
            }
            return current;
        });
        IdempotencyRecord snapshot = record.snapshot();
        if (!record.fingerprint.equals(fingerprint)) {
            return new IdempotencyDecision(IdempotencyDecisionType.FINGERPRINT_MISMATCH, snapshot);
        }
        if (created.get() && record.status == IdempotencyStatus.STARTED) {
            return new IdempotencyDecision(IdempotencyDecisionType.STARTED, snapshot);
        }
        if (record.status == IdempotencyStatus.STARTED) {
            return new IdempotencyDecision(IdempotencyDecisionType.IN_PROGRESS, snapshot);
        }
        return new IdempotencyDecision(IdempotencyDecisionType.REPLAY, snapshot);
    }

    @Override
    public void complete(String key, int statusCode) {
        update(key, IdempotencyStatus.COMPLETED, statusCode);
    }

    @Override
    public void fail(String key, int statusCode) {
        update(key, IdempotencyStatus.FAILED, statusCode);
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        MutableIdempotencyRecord record = records.get(key);
        if (record == null || record.expiresAt.isBefore(Instant.now())) {
            records.remove(key);
            return Optional.empty();
        }
        return Optional.of(record.snapshot());
    }

    private void update(String key, IdempotencyStatus status, int statusCode) {
        MutableIdempotencyRecord record = records.get(key);
        if (record != null) {
            record.status = status;
            record.statusCode = statusCode;
            record.updatedAt = Instant.now();
        }
    }

    private static final class MutableIdempotencyRecord {
        private final String key;
        private final String fingerprint;
        private IdempotencyStatus status;
        private Integer statusCode;
        private final Instant createdAt;
        private Instant updatedAt;
        private final Instant expiresAt;

        private MutableIdempotencyRecord(String key, String fingerprint, IdempotencyStatus status, Integer statusCode,
                                         Instant createdAt, Instant updatedAt, Instant expiresAt) {
            this.key = key;
            this.fingerprint = fingerprint;
            this.status = status;
            this.statusCode = statusCode;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.expiresAt = expiresAt;
        }

        private IdempotencyRecord snapshot() {
            return new IdempotencyRecord(key, fingerprint, status, statusCode, createdAt, updatedAt, expiresAt);
        }
    }
}

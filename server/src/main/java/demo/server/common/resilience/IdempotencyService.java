package demo.server.common.resilience;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyService {

    IdempotencyDecision begin(String key, String fingerprint, Duration ttl);

    void complete(String key, int statusCode);

    void fail(String key, int statusCode);

    Optional<IdempotencyRecord> find(String key);
}

package demo.server.common.resilience;

import java.time.Instant;

public record RateLimitDecision(
        boolean allowed,
        int remaining,
        Instant resetAt
) {
}

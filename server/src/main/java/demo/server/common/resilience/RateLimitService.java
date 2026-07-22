package demo.server.common.resilience;

import java.time.Duration;

public interface RateLimitService {

    RateLimitDecision consume(String key, int permits, Duration window);
}

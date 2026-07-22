package demo.server.common.resilience;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InMemoryRateLimitService implements RateLimitService {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public RateLimitDecision consume(String key, int permits, Duration windowDuration) {
        Instant now = Instant.now();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.resetAt().isBefore(now)) {
                return new Window(new AtomicInteger(1), now.plus(windowDuration));
            }
            current.count().incrementAndGet();
            return current;
        });
        int used = window.count().get();
        return new RateLimitDecision(used <= permits, Math.max(permits - used, 0), window.resetAt());
    }

    private record Window(AtomicInteger count, Instant resetAt) {
    }
}

package demo.server.common.resilience;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryDistributedLockService implements DistributedLockService {

    private final ConcurrentHashMap<String, LockEntry> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<LockHandle> tryAcquire(String key, Duration ttl) {
        Instant now = Instant.now();
        String owner = UUID.randomUUID().toString();
        LockEntry entry = locks.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt().isBefore(now)) {
                return new LockEntry(owner, now.plus(ttl));
            }
            return current;
        });
        if (!entry.owner().equals(owner)) {
            return Optional.empty();
        }
        return Optional.of(new InMemoryLockHandle(key, owner));
    }

    private record LockEntry(String owner, Instant expiresAt) {
    }

    private final class InMemoryLockHandle implements LockHandle {
        private final String key;
        private final String owner;

        private InMemoryLockHandle(String key, String owner) {
            this.key = key;
            this.owner = owner;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public void close() {
            locks.computeIfPresent(key, (ignored, current) -> current.owner().equals(owner) ? null : current);
        }
    }
}

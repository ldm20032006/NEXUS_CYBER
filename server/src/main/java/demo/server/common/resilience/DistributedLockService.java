package demo.server.common.resilience;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public interface DistributedLockService {

    Optional<LockHandle> tryAcquire(String key, Duration ttl);

    default <T> Optional<T> tryWithLock(String key, Duration ttl, Supplier<T> supplier) {
        Optional<LockHandle> lock = tryAcquire(key, ttl);
        if (lock.isEmpty()) {
            return Optional.empty();
        }
        try (LockHandle ignored = lock.get()) {
            return Optional.ofNullable(supplier.get());
        }
    }
}

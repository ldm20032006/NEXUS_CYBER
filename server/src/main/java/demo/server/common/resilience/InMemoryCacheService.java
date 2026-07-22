package demo.server.common.resilience;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryCacheService implements CacheService {

    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    @Override
    public <T> Optional<T> get(String key, Class<T> valueType) {
        CacheEntry entry = entries.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key);
            return Optional.empty();
        }
        if (!valueType.isInstance(entry.value())) {
            return Optional.empty();
        }
        return Optional.of(valueType.cast(entry.value()));
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        entries.put(key, new CacheEntry(value, Instant.now().plus(ttl)));
    }

    @Override
    public void evict(String key) {
        entries.remove(key);
    }

    private record CacheEntry(Object value, Instant expiresAt) {
    }
}

package demo.server.common.resilience;

import java.time.Duration;
import java.util.Optional;

public interface CacheService {

    <T> Optional<T> get(String key, Class<T> valueType);

    void put(String key, Object value, Duration ttl);

    void evict(String key);
}

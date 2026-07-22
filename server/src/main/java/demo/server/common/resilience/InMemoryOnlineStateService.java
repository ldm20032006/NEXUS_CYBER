package demo.server.common.resilience;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryOnlineStateService implements OnlineStateService {

    private final ConcurrentHashMap<String, Instant> onlineUntil = new ConcurrentHashMap<>();

    @Override
    public void markOnline(String key, Duration ttl) {
        onlineUntil.put(key, Instant.now().plus(ttl));
    }

    @Override
    public void markOffline(String key) {
        onlineUntil.remove(key);
    }

    @Override
    public boolean isOnline(String key) {
        Instant expiresAt = onlineUntil.get(key);
        if (expiresAt == null || expiresAt.isBefore(Instant.now())) {
            onlineUntil.remove(key);
            return false;
        }
        return true;
    }
}

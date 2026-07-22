package demo.server.common.resilience;

import java.time.Duration;

public interface OnlineStateService {

    void markOnline(String key, Duration ttl);

    void markOffline(String key);

    boolean isOnline(String key);
}

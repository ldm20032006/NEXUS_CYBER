package demo.server.common.security;

import java.util.Optional;
import java.util.UUID;

public interface CurrentUserProvider {

    Optional<UUID> currentUserId();

    Optional<String> currentUsername();

    boolean isAuthenticated();
}

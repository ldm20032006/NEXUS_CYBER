package demo.server.common.websocket;

import demo.server.common.enums.RoleCode;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

public record WebSocketPrincipal(
        String name,
        UUID userId,
        UUID stationId,
        UUID branchId,
        Set<RoleCode> roles,
        WebSocketPrincipalType type
) implements Principal {

    public static WebSocketPrincipal user(UUID userId, UUID branchId, Set<RoleCode> roles) {
        return new WebSocketPrincipal(userId.toString(), userId, null, branchId, roles, WebSocketPrincipalType.USER);
    }

    public static WebSocketPrincipal station(UUID stationId, UUID branchId) {
        return new WebSocketPrincipal("station:" + stationId, null, stationId, branchId, Set.of(), WebSocketPrincipalType.STATION);
    }

    @Override
    public String getName() {
        return name;
    }
}

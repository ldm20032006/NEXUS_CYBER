package demo.server.common.websocket;

import java.util.UUID;

public final class WebSocketTopics {

    public static final String USER_QUEUE_NOTIFICATIONS = "/user/queue/notifications";

    private WebSocketTopics() {
    }

    public static String user(UUID userId) {
        return "/topic/users/" + userId;
    }

    public static String station(UUID stationId) {
        return "/topic/stations/" + stationId;
    }

    public static String branchOrders(UUID branchId) {
        return "/topic/branches/" + branchId + "/orders";
    }

    public static String branchAlerts(UUID branchId) {
        return "/topic/branches/" + branchId + "/alerts";
    }

    public static String lobby(UUID lobbyId) {
        return "/topic/lobbies/" + lobbyId;
    }
}

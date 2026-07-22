package demo.server.common.websocket;

import demo.server.common.enums.LobbyMemberStatus;
import demo.server.common.enums.RoleCode;
import demo.server.entity.branch.Station;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.lobby.LobbyMemberRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WebSocketSubscriptionAuthorizer {

    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/users/([0-9a-fA-F-]{36})$");
    private static final Pattern STATION_TOPIC = Pattern.compile("^/topic/stations/([0-9a-fA-F-]{36})$");
    private static final Pattern BRANCH_TOPIC = Pattern.compile("^/topic/branches/([0-9a-fA-F-]{36})/(orders|alerts)$");
    private static final Pattern LOBBY_TOPIC = Pattern.compile("^/topic/lobbies/([0-9a-fA-F-]{36})$");

    private final StationRepository stationRepository;
    private final LobbyMemberRepository lobbyMemberRepository;

    public WebSocketSubscriptionAuthorizer(StationRepository stationRepository, LobbyMemberRepository lobbyMemberRepository) {
        this.stationRepository = stationRepository;
        this.lobbyMemberRepository = lobbyMemberRepository;
    }

    public boolean canSubscribe(WebSocketPrincipal principal, String destination) {
        if (principal == null || destination == null) {
            return false;
        }
        if (WebSocketTopics.USER_QUEUE_NOTIFICATIONS.equals(destination)) {
            return principal.type() == WebSocketPrincipalType.USER;
        }
        Matcher userMatcher = USER_TOPIC.matcher(destination);
        if (userMatcher.matches()) {
            return principal.type() == WebSocketPrincipalType.USER
                    && uuid(userMatcher.group(1)).map(id -> id.equals(principal.userId())).orElse(false);
        }
        Matcher stationMatcher = STATION_TOPIC.matcher(destination);
        if (stationMatcher.matches()) {
            return uuid(stationMatcher.group(1)).map(stationId -> canSubscribeStation(principal, stationId)).orElse(false);
        }
        Matcher branchMatcher = BRANCH_TOPIC.matcher(destination);
        if (branchMatcher.matches()) {
            return uuid(branchMatcher.group(1)).map(branchId -> canSubscribeBranch(principal, branchId)).orElse(false);
        }
        Matcher lobbyMatcher = LOBBY_TOPIC.matcher(destination);
        if (lobbyMatcher.matches()) {
            return principal.type() == WebSocketPrincipalType.USER
                    && uuid(lobbyMatcher.group(1)).map(lobbyId -> isLobbyMember(lobbyId, principal.userId())).orElse(false);
        }
        return false;
    }

    private boolean canSubscribeStation(WebSocketPrincipal principal, UUID stationId) {
        if (principal.type() == WebSocketPrincipalType.STATION) {
            return stationId.equals(principal.stationId());
        }
        Optional<Station> station = stationRepository.findById(stationId).filter(item -> !item.isDeleted());
        return station.map(value -> canSubscribeBranch(principal, value.getBranch().getId())).orElse(false);
    }

    private boolean canSubscribeBranch(WebSocketPrincipal principal, UUID branchId) {
        if (principal.type() == WebSocketPrincipalType.STATION) {
            return branchId.equals(principal.branchId());
        }
        return principal.roles().contains(RoleCode.SUPER_ADMIN) || branchId.equals(principal.branchId());
    }

    private boolean isLobbyMember(UUID lobbyId, UUID userId) {
        return lobbyMemberRepository.findByLobby_IdAndUser_Id(lobbyId, userId)
                .filter(member -> !member.isDeleted())
                .filter(member -> member.getStatus() == LobbyMemberStatus.ACTIVE)
                .isPresent();
    }

    private Optional<UUID> uuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}

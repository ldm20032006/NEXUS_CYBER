package demo.server.websocket;

import demo.server.common.enums.GameStatus;
import demo.server.common.enums.LobbyMemberRole;
import demo.server.common.enums.LobbyMemberStatus;
import demo.server.common.enums.LobbyStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.TokenHashService;
import demo.server.common.websocket.WebSocketInboundInterceptor;
import demo.server.common.websocket.WebSocketPrincipal;
import demo.server.common.websocket.WebSocketTopics;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.game.Game;
import demo.server.entity.lobby.Lobby;
import demo.server.entity.lobby.LobbyMember;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.lobby.LobbyMemberRepository;
import demo.server.repository.lobby.LobbyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:nexus-ws-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class WebSocketSubscriptionAuthorizationTests {

    @Autowired
    WebSocketInboundInterceptor interceptor;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    StationRepository stationRepository;

    @Autowired
    StationCredentialRepository credentialRepository;

    @Autowired
    TokenHashService tokenHashService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    LobbyRepository lobbyRepository;

    @Autowired
    LobbyMemberRepository lobbyMemberRepository;

    Branch branchA;
    Branch branchB;
    AppUser gamer;
    AppUser other;
    Station stationA;
    Station stationB;

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
        branchA = branch("A");
        branchB = branch("B");
        gamer = user("gamer-ws@example.com", RoleCode.GAMER, branchA);
        other = user("other-ws@example.com", RoleCode.GAMER, branchB);
        stationA = station(branchA, "PC01");
        stationB = station(branchB, "PC02");
    }

    @Test
    void userCanSubscribeOwnUserTopicOnly() {
        WebSocketPrincipal principal = WebSocketPrincipal.user(gamer.getId(), branchA.getId(), Set.of(RoleCode.GAMER));

        assertThatNoException().isThrownBy(() -> subscribe(principal, WebSocketTopics.user(gamer.getId())));
        assertThatThrownBy(() -> subscribe(principal, WebSocketTopics.user(other.getId())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void stationCredentialCanSubscribeOwnStationTopicOnly() {
        String secret = "station-secret-value";
        credential(stationA, secret);
        Map<String, Object> session = new HashMap<>();
        connectStation(session, stationA.getId(), secret);

        assertThatNoException().isThrownBy(() -> subscribe(session, WebSocketTopics.station(stationA.getId())));
        assertThatThrownBy(() -> subscribe(session, WebSocketTopics.station(stationB.getId())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void branchTopicRequiresMatchingBranchUnlessSuperAdmin() {
        WebSocketPrincipal branchUser = WebSocketPrincipal.user(gamer.getId(), branchA.getId(), Set.of(RoleCode.BRANCH_ADMIN));
        WebSocketPrincipal superAdmin = WebSocketPrincipal.user(UUID.randomUUID(), null, Set.of(RoleCode.SUPER_ADMIN));

        assertThatNoException().isThrownBy(() -> subscribe(branchUser, WebSocketTopics.branchOrders(branchA.getId())));
        assertThatThrownBy(() -> subscribe(branchUser, WebSocketTopics.branchAlerts(branchB.getId())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatNoException().isThrownBy(() -> subscribe(superAdmin, WebSocketTopics.branchAlerts(branchB.getId())));
    }

    @Test
    void lobbyTopicRequiresActiveMember() {
        Lobby lobby = lobby(gamer);
        LobbyMember member = new LobbyMember();
        member.setLobby(lobby);
        member.setUser(gamer);
        member.setRole(LobbyMemberRole.MEMBER);
        member.setStatus(LobbyMemberStatus.ACTIVE);
        member.setJoinedAt(Instant.now());
        lobbyMemberRepository.save(member);

        WebSocketPrincipal memberPrincipal = WebSocketPrincipal.user(gamer.getId(), branchA.getId(), Set.of(RoleCode.GAMER));
        WebSocketPrincipal outsiderPrincipal = WebSocketPrincipal.user(other.getId(), branchB.getId(), Set.of(RoleCode.GAMER));

        assertThatNoException().isThrownBy(() -> subscribe(memberPrincipal, WebSocketTopics.lobby(lobby.getId())));
        assertThatThrownBy(() -> subscribe(outsiderPrincipal, WebSocketTopics.lobby(lobby.getId())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void userQueueNotificationsRequiresAuthenticatedUserPrincipal() {
        WebSocketPrincipal userPrincipal = WebSocketPrincipal.user(gamer.getId(), branchA.getId(), Set.of(RoleCode.GAMER));
        WebSocketPrincipal stationPrincipal = WebSocketPrincipal.station(stationA.getId(), branchA.getId());

        assertThatNoException().isThrownBy(() -> subscribe(userPrincipal, WebSocketTopics.USER_QUEUE_NOTIFICATIONS));
        assertThatThrownBy(() -> subscribe(stationPrincipal, WebSocketTopics.USER_QUEUE_NOTIFICATIONS))
                .isInstanceOf(AccessDeniedException.class);
    }

    private void connectStation(Map<String, Object> session, UUID stationId, String secret) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(session);
        accessor.addNativeHeader("X-Station-Id", stationId.toString());
        accessor.addNativeHeader("X-Station-Secret", secret);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, null);
    }

    private void subscribe(WebSocketPrincipal principal, String destination) {
        Map<String, Object> session = new HashMap<>();
        session.put("nexusWsPrincipal", principal);
        subscribe(session, destination);
    }

    private void subscribe(Map<String, Object> session, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionAttributes(session);
        accessor.setDestination(destination);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, null);
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(code + " Branch");
        branch.setTimezone("Asia/Ho_Chi_Minh");
        branch.setPaymentPolicy("PREPAID_OR_WALLET");
        return branchRepository.save(branch);
    }

    private Station station(Branch branch, String code) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber("PC01".equals(code) ? 1 : 2);
        return stationRepository.save(station);
    }

    private StationCredential credential(Station station, String rawSecret) {
        StationCredential credential = new StationCredential();
        credential.setStation(station);
        credential.setSecretHash(tokenHashService.hash(rawSecret));
        credential.setIssuedAt(Instant.now());
        return credentialRepository.save(credential);
    }

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }

    private Lobby lobby(AppUser leader) {
        Game game = new Game();
        game.setSlug("ws-game");
        game.setName("WS Game");
        game.setMaxLobbySize(5);
        game.setStatus(GameStatus.ACTIVE);
        gameRepository.save(game);
        Lobby lobby = new Lobby();
        lobby.setCreator(leader);
        lobby.setLeader(leader);
        lobby.setGame(game);
        lobby.setBranch(leader.getBranch());
        lobby.setName("Lobby");
        lobby.setMaxMembers(5);
        lobby.setStatus(LobbyStatus.OPEN);
        return lobbyRepository.save(lobby);
    }
}

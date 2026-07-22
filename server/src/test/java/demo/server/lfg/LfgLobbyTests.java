package demo.server.lfg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.GameStatus;
import demo.server.common.enums.InvitationStatus;
import demo.server.common.enums.LfgSignalStatus;
import demo.server.common.enums.LobbyMemberStatus;
import demo.server.common.enums.LobbyStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.UserStatus;
import demo.server.common.enums.VoiceChannelStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.entity.game.GamerGameProfile;
import demo.server.entity.lfg.LfgSignal;
import demo.server.entity.lfg.TeamInvitation;
import demo.server.entity.lobby.Lobby;
import demo.server.entity.session.PlaySession;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.game.GameRankRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.game.GameRoleRepository;
import demo.server.repository.game.GamerGameProfileRepository;
import demo.server.repository.lfg.LfgSignalRepository;
import demo.server.repository.lfg.TeamInvitationRepository;
import demo.server.repository.lobby.LobbyMemberRepository;
import demo.server.repository.lobby.LobbyRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.service.lfg.VoiceService;
import demo.server.voice.VoiceChannel;
import demo.server.voice.VoiceProviderException;
import demo.server.voice.VoiceProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalTime;
import java.time.Duration;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:nexus-lfg-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "nexus.voice.webhook-secret=test-voice-webhook-secret",
        "nexus.voice.token-ttl=PT1S"
})
class LfgLobbyTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired StationRepository stationRepository;
    @Autowired PlaySessionRepository sessionRepository;
    @Autowired GameRepository gameRepository;
    @Autowired GameRankRepository rankRepository;
    @Autowired GameRoleRepository gameRoleRepository;
    @Autowired GamerGameProfileRepository gameProfileRepository;
    @Autowired LfgSignalRepository lfgRepository;
    @Autowired TeamInvitationRepository invitationRepository;
    @Autowired LobbyRepository lobbyRepository;
    @Autowired LobbyMemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired VoiceService voiceService;

    Branch branch;
    Station station;
    Game game;
    GameRank rank;
    GameRole role;
    int stationSequence;

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role roleEntity = new Role();
            roleEntity.setCode(code);
            roleEntity.setName(code.name());
            roleRepository.save(roleEntity);
        });
        branch = branch("LFG01");
        station = station(branch, "PC01");
        stationSequence = 2;
        game = game("arena", 2);
        rank = rank(game, "GOLD");
        role = gameRole(game, "DUO");
    }

    @Test
    void lfgRequiresActiveSessionAndGameProfileThenFiltersBlockedCandidates() throws Exception {
        AppUser viewer = gamer("lfg-viewer@example.com");
        AppUser candidate = gamer("lfg-candidate@example.com");
        activeSession(viewer);
        activeSession(candidate);
        gameProfile(viewer, game, rank, role);
        gameProfile(candidate, game, rank, role);
        String candidateToken = token(candidate);
        createSignal(candidateToken, 200);
        String viewerToken = token(viewer);

        mockMvc.perform(get("/api/v1/lfg/signals")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("branchId", branch.getId().toString())
                        .param("gameId", game.getId().toString())
                        .param("rankId", rank.getId().toString())
                        .param("roleId", role.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(candidate.getId().toString()));

        mockMvc.perform(post("/api/v1/social/blocks/{userId}", candidate.getId())
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/lfg/signals")
                        .header("Authorization", "Bearer " + viewerToken)
                        .param("branchId", branch.getId().toString())
                        .param("gameId", game.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void expiredInvitationCannotBeAccepted() throws Exception {
        AppUser sender = gamer("invite-expired-sender@example.com");
        AppUser receiver = gamer("invite-expired-receiver@example.com");
        activeSession(sender);
        activeSession(receiver);
        gameProfile(sender, game, rank, role);
        gameProfile(receiver, game, rank, role);
        JsonNode invitation = invite(token(sender), receiver.getId(), null, 200);
        TeamInvitation entity = invitationRepository.findById(UUID.fromString(invitation.path("data").path("id").asText())).orElseThrow();
        entity.setExpiresAt(Instant.now().minusSeconds(1));
        invitationRepository.save(entity);

        mockMvc.perform(patch("/api/v1/team-invitations/{id}/accept", entity.getId())
                        .header("Authorization", "Bearer " + token(receiver)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(invitationRepository.findById(entity.getId()).orElseThrow().getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @Test
    void concurrentAcceptOnlyAddsMemberOnceAndCapacityIsEnforced() throws Exception {
        AppUser leader = gamer("lobby-leader@example.com");
        AppUser receiver = gamer("lobby-receiver@example.com");
        AppUser third = gamer("lobby-third@example.com");
        activeSession(leader);
        activeSession(receiver);
        activeSession(third);
        gameProfile(leader, game, rank, role);
        gameProfile(receiver, game, rank, role);
        gameProfile(third, game, rank, role);
        JsonNode invitation = invite(token(leader), receiver.getId(), null, 200);
        UUID invitationId = UUID.fromString(invitation.path("data").path("id").asText());
        String receiverToken = token(receiver);

        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> first = () -> acceptStatus(receiverToken, invitationId);
            Callable<Integer> second = () -> acceptStatus(receiverToken, invitationId);
            List<Integer> statuses = executor.invokeAll(List.of(first, second)).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }).toList();
            assertThat(statuses).contains(200);
            Lobby lobby = lobbyRepository.findAll().getFirst();
            assertThat(memberRepository.countByLobby_IdAndStatus(lobby.getId(), LobbyMemberStatus.ACTIVE)).isEqualTo(2);
            invite(token(leader), third.getId(), lobby.getId(), 200);
            UUID thirdInvitationId = invitationRepository.findBySenderIdOrReceiverId(leader.getId(), third.getId()).stream()
                    .filter(item -> item.getReceiver().getId().equals(third.getId()))
                    .findFirst().orElseThrow().getId();
            mockMvc.perform(patch("/api/v1/team-invitations/{id}/accept", thirdInvitationId)
                            .header("Authorization", "Bearer " + token(third)))
                    .andExpect(status().isUnprocessableEntity());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void onlyMembersCanViewLobbyAndChatIsSanitized() throws Exception {
        AppUser leader = gamer("chat-leader@example.com");
        AppUser member = gamer("chat-member@example.com");
        AppUser outsider = gamer("chat-outsider@example.com");
        activeSession(leader);
        activeSession(member);
        activeSession(outsider);
        gameProfile(leader, game, rank, role);
        gameProfile(member, game, rank, role);
        gameProfile(outsider, game, rank, role);
        String leaderToken = token(leader);
        String memberToken = token(member);
        String outsiderToken = token(outsider);
        JsonNode invitation = invite(leaderToken, member.getId(), null, 200);
        JsonNode lobbyResponse = accept(memberToken, UUID.fromString(invitation.path("data").path("id").asText()), 200);
        UUID lobbyId = UUID.fromString(lobbyResponse.path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/lobbies/{id}", lobbyId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/lobbies/{id}/messages", lobbyId)
                        .header("Authorization", "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"<b>Hello</b> <script>alert(1)</script>"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Hello alert(1)"));

        mockMvc.perform(get("/api/v1/lobbies/{id}/messages", lobbyId)
                        .header("Authorization", "Bearer " + memberToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("Hello alert(1)"));

        mockMvc.perform(delete("/api/v1/lobbies/{id}/members/{userId}", lobbyId, leader.getId())
                        .header("Authorization", "Bearer " + memberToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionEndedEventClosesActiveLfg() throws Exception {
        AppUser user = gamer("lfg-session-end@example.com");
        PlaySession session = activeSession(user);
        gameProfile(user, game, rank, role);
        JsonNode signal = createSignal(token(user), 200);
        eventPublisher.publishEvent(new demo.server.service.lfg.LfgLobbyService.SessionEndedEvent(session.getId()));

        assertThat(lfgRepository.findById(UUID.fromString(signal.path("data").path("id").asText())).orElseThrow().getStatus())
                .isEqualTo(LfgSignalStatus.CANCELLED);
    }

    @Test
    void onlyActiveLobbyMemberCanRequestVoiceTokenAndTokenIsBoundToUserAndLobby() throws Exception {
        AppUser leader = gamer("voice-leader@example.com");
        AppUser outsider = gamer("voice-outsider@example.com");
        activeSession(leader);
        activeSession(outsider);
        gameProfile(leader, game, rank, role);
        gameProfile(outsider, game, rank, role);
        JsonNode lobby = createLobby(token(leader), 200);
        UUID lobbyId = UUID.fromString(lobby.path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/lobbies/{id}/voice/token", lobbyId)
                        .header("Authorization", "Bearer " + token(outsider)))
                .andExpect(status().isForbidden());

        JsonNode response = voiceToken(token(leader), lobbyId, 200);
        assertThat(response.path("data").path("userId").asText()).isEqualTo(leader.getId().toString());
        assertThat(response.path("data").path("lobbyId").asText()).isEqualTo(lobbyId.toString());
        assertThat(response.path("data").path("status").asText()).isEqualTo("ACTIVE");
        String tokenPayload = new String(Base64.getUrlDecoder().decode(response.path("data").path("token").asText()), StandardCharsets.UTF_8);
        assertThat(tokenPayload).contains(lobbyId.toString(), leader.getId().toString());
        assertThat(Instant.parse(response.path("data").path("expiresAt").asText())).isBeforeOrEqualTo(Instant.now().plusSeconds(2));
    }

    @Test
    void voiceTokenExpiresAndLeftMemberCannotRequestNewToken() throws Exception {
        AppUser leader = gamer("voice-left-leader@example.com");
        AppUser member = gamer("voice-left-member@example.com");
        activeSession(leader);
        activeSession(member);
        gameProfile(leader, game, rank, role);
        gameProfile(member, game, rank, role);
        JsonNode invitation = invite(token(leader), member.getId(), null, 200);
        JsonNode lobby = accept(token(member), UUID.fromString(invitation.path("data").path("id").asText()), 200);
        UUID lobbyId = UUID.fromString(lobby.path("data").path("id").asText());

        JsonNode tokenResponse = voiceToken(token(member), lobbyId, 200);
        Instant expiresAt = Instant.parse(tokenResponse.path("data").path("expiresAt").asText());
        Thread.sleep(Duration.ofMillis(1200));
        assertThat(expiresAt).isBefore(Instant.now());

        mockMvc.perform(delete("/api/v1/lobbies/{id}/members/me", lobbyId)
                        .header("Authorization", "Bearer " + token(member)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/lobbies/{id}/voice/token", lobbyId)
                        .header("Authorization", "Bearer " + token(member)))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerFailureReturnsVoiceUnavailableButLobbyAndTextChatStillWork() throws Exception {
        AppUser leader = gamer("voice-failure-leader@example.com");
        activeSession(leader);
        gameProfile(leader, game, rank, role);
        JsonNode lobby = createLobby(token(leader), 200);
        UUID lobbyId = UUID.fromString(lobby.path("data").path("id").asText());
        ReflectionTestUtils.setField(voiceService, "voiceProvider", failingVoiceProvider());

        mockMvc.perform(post("/api/v1/lobbies/{id}/voice/token", lobbyId)
                        .header("Authorization", "Bearer " + token(leader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("VOICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.status").value("VOICE_UNAVAILABLE"));

        mockMvc.perform(get("/api/v1/lobbies/{id}", lobbyId)
                        .header("Authorization", "Bearer " + token(leader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));
        mockMvc.perform(post("/api/v1/lobbies/{id}/messages", lobbyId)
                        .header("Authorization", "Bearer " + token(leader))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"text chat still works\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("text chat still works"));
    }

    @Test
    void voiceWebhookRejectsBadSignatureAndDeduplicatesReplay() throws Exception {
        String timestamp = Instant.now().toString();
        String body = "{\"eventId\":\"voice-event-1\",\"eventType\":\"participant.joined\",\"lobbyId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/v1/lobbies/voice/webhooks/mock")
                        .header("X-Voice-Timestamp", timestamp)
                        .header("X-Voice-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        String signature = hmacSha256Hex("test-voice-webhook-secret", timestamp + "." + body);
        mockMvc.perform(post("/api/v1/lobbies/voice/webhooks/mock")
                        .header("X-Voice-Timestamp", timestamp)
                        .header("X-Voice-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(true));
        mockMvc.perform(post("/api/v1/lobbies/voice/webhooks/mock")
                        .header("X-Voice-Timestamp", timestamp)
                        .header("X-Voice-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processed").value(false));
    }

    @Test
    void disbandClosesVoiceChannelWhenProviderSupportsIt() throws Exception {
        AppUser leader = gamer("voice-close-leader@example.com");
        activeSession(leader);
        gameProfile(leader, game, rank, role);
        JsonNode lobby = createLobby(token(leader), 200);
        UUID lobbyId = UUID.fromString(lobby.path("data").path("id").asText());
        voiceToken(token(leader), lobbyId, 200);

        mockMvc.perform(delete("/api/v1/lobbies/{id}", lobbyId)
                        .header("Authorization", "Bearer " + token(leader)))
                .andExpect(status().isOk());
        Lobby closed = lobbyRepository.findById(lobbyId).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(LobbyStatus.CLOSED);
        assertThat(closed.getVoiceStatus()).isEqualTo(VoiceChannelStatus.CLOSED);
    }

    private JsonNode createSignal(String token, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/lfg/signals")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameId":"%s","rankId":"%s","roleId":"%s","targetMembers":2,"message":"duo"}
                                """.formatted(game.getId(), rank.getId(), role.getId())))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode invite(String token, UUID receiverId, UUID lobbyId, int expectedStatus) throws Exception {
        String body = lobbyId == null
                ? """
                {"receiverId":"%s","message":"join"}
                """.formatted(receiverId)
                : """
                {"receiverId":"%s","lobbyId":"%s","message":"join"}
                """.formatted(receiverId, lobbyId);
        String response = mockMvc.perform(post("/api/v1/team-invitations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode accept(String token, UUID invitationId, int expectedStatus) throws Exception {
        String response = mockMvc.perform(patch("/api/v1/team-invitations/{id}/accept", invitationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode createLobby(String token, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/lobbies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameId":"%s","name":"voice lobby"}
                                """.formatted(game.getId())))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode voiceToken(String token, UUID lobbyId, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/lobbies/{id}/voice/token", lobbyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private VoiceProviderPort failingVoiceProvider() {
        return new VoiceProviderPort() {
            @Override
            public VoiceChannel createChannel(UUID lobbyId, String lobbyName) {
                throw new VoiceProviderException("voice down");
            }

            @Override
            public VoiceToken issueToken(String channelId, UUID lobbyId, UUID userId, Duration ttl) {
                throw new VoiceProviderException("voice down");
            }

            @Override
            public void closeChannel(String channelId) {
                throw new VoiceProviderException("voice down");
            }

            @Override
            public String providerName() {
                return "mock";
            }

            @Override
            public String adapterMode() {
                return "test-failing-mock";
            }
        };
    }

    private String hmacSha256Hex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormatHolder.HEX.formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static class HexFormatHolder {
        static final java.util.HexFormat HEX = java.util.HexFormat.of();
    }

    private int acceptStatus(String token, UUID invitationId) throws Exception {
        return mockMvc.perform(patch("/api/v1/team-invitations/{id}/accept", invitationId)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private String token(AppUser user) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"Password123"}
                                """.formatted(user.getEmail())))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(code);
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setTimezone("Asia/Ho_Chi_Minh");
        branch.setPaymentPolicy("PREPAID_OR_WALLET");
        branch.setOperatingStartTime(LocalTime.of(8, 0));
        branch.setOperatingEndTime(LocalTime.of(23, 0));
        return branchRepository.save(branch);
    }

    private Station station(Branch branch, String code) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber(Integer.parseInt(code.substring(2)));
        station.setStatus(StationStatus.OCCUPIED);
        return stationRepository.save(station);
    }

    private Game game(String slug, int maxLobbySize) {
        Game game = new Game();
        game.setSlug(slug);
        game.setName(slug);
        game.setMaxLobbySize(maxLobbySize);
        game.setStatus(GameStatus.ACTIVE);
        return gameRepository.save(game);
    }

    private GameRank rank(Game game, String code) {
        GameRank rank = new GameRank();
        rank.setGame(game);
        rank.setCode(code);
        rank.setName(code);
        return rankRepository.save(rank);
    }

    private GameRole gameRole(Game game, String code) {
        GameRole role = new GameRole();
        role.setGame(game);
        role.setCode(code);
        role.setName(code);
        return gameRoleRepository.save(role);
    }

    private AppUser gamer(String email) {
        Role role = roleRepository.findByCode(RoleCode.GAMER).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName(email);
        user.setStatus(UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }

    private PlaySession activeSession(AppUser user) {
        Station userStation = station(branch, "PC" + stationSequence++);
        PlaySession session = new PlaySession();
        session.setUser(user);
        session.setStation(userStation);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        return sessionRepository.save(session);
    }

    private GamerGameProfile gameProfile(AppUser user, Game game, GameRank rank, GameRole role) {
        GamerGameProfile profile = new GamerGameProfile();
        profile.setUser(user);
        profile.setGame(game);
        profile.setRank(rank);
        profile.setPreferredRole(role);
        profile.setInGameName(user.getEmail());
        profile.setVisibleOnRadar(true);
        return gameProfileRepository.save(profile);
    }
}

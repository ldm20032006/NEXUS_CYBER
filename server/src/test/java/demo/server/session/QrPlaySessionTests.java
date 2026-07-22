package demo.server.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.QrLoginSessionStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.TokenHashService;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.session.QrLoginSession;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.session.QrLoginSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:nexus-session-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class QrPlaySessionTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
    QrLoginSessionRepository qrRepository;

    @Autowired
    PlaySessionRepository sessionRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    TokenHashService tokenHashService;

    Branch branch;
    Station station;
    String stationSecret = "station-qr-secret";

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
        branch = branch("HN01", "PREPAID_OR_WALLET");
        station = station(branch, "PC01", StationStatus.AVAILABLE);
        credential(station, stationSecret);
    }

    @Test
    void stationCredentialCreatesQrWithoutSensitivePayloadAndGamerConfirmStartsSession() throws Exception {
        AppUser gamer = user("gamer-session@example.com", UserStatus.ACTIVE);
        String token = token(gamer);
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-create-1");

        assertThat(qr.path("data").path("qrPayload").asText()).contains("qrSessionId", "nonce").doesNotContain("Bearer", "secret", "jwt");
        UUID qrId = UUID.fromString(qr.path("data").path("qrSessionId").asText());
        String nonce = qr.path("data").path("nonce").asText();

        JsonNode session = confirm(qrId, nonce, token, "confirm-1", 200);

        assertThat(session.path("data").path("status").asText()).isEqualTo("ACTIVE");
        assertThat(stationRepository.findById(station.getId()).orElseThrow().getStatus()).isEqualTo(StationStatus.OCCUPIED);
        assertThat(qrRepository.findById(qrId).orElseThrow().getStatus()).isEqualTo(QrLoginSessionStatus.USED);
    }

    @Test
    void expiredQrIsRejected() throws Exception {
        String token = token(user("expired@example.com", UserStatus.ACTIVE));
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-expired");
        UUID qrId = UUID.fromString(qr.path("data").path("qrSessionId").asText());
        String nonce = qr.path("data").path("nonce").asText();
        QrLoginSession qrEntity = qrRepository.findById(qrId).orElseThrow();
        qrEntity.setExpiresAt(Instant.now().minusSeconds(1));
        qrRepository.save(qrEntity);

        confirm(qrId, nonce, token, "confirm-expired", 422);
    }

    @Test
    void usedQrCannotCreateSecondSession() throws Exception {
        String token = token(user("used@example.com", UserStatus.ACTIVE));
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-used");
        UUID qrId = UUID.fromString(qr.path("data").path("qrSessionId").asText());
        String nonce = qr.path("data").path("nonce").asText();

        confirm(qrId, nonce, token, "confirm-used-1", 200);
        confirm(qrId, nonce, token, "confirm-used-2", 422);
    }

    @Test
    void duplicateActiveSessionIsRejectedAndEndSessionReleasesStation() throws Exception {
        AppUser gamer = user("duplicate-active@example.com", UserStatus.ACTIVE);
        String token = token(gamer);
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-duplicate-1");
        JsonNode active = confirm(UUID.fromString(qr.path("data").path("qrSessionId").asText()), qr.path("data").path("nonce").asText(), token, "confirm-duplicate-1", 200);

        Station secondStation = station(branch, "PC02", StationStatus.AVAILABLE);
        credential(secondStation, "second-secret");
        JsonNode secondQr = createQr(secondStation.getId(), "second-secret", "qr-duplicate-2");
        confirm(UUID.fromString(secondQr.path("data").path("qrSessionId").asText()), secondQr.path("data").path("nonce").asText(), token, "confirm-duplicate-2", 422);

        UUID sessionId = UUID.fromString(active.path("data").path("id").asText());
        mockMvc.perform(post("/api/v1/sessions/{id}/end", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"done"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        assertThat(stationRepository.findById(station.getId()).orElseThrow().getStatus()).isEqualTo(StationStatus.AVAILABLE);
    }

    @Test
    void sessionScopePreventsOtherUserEndingSession() throws Exception {
        String ownerToken = token(user("owner-session@example.com", UserStatus.ACTIVE));
        String otherToken = token(user("other-session@example.com", UserStatus.ACTIVE));
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-scope");
        JsonNode active = confirm(UUID.fromString(qr.path("data").path("qrSessionId").asText()), qr.path("data").path("nonce").asText(), ownerToken, "confirm-scope", 200);

        mockMvc.perform(post("/api/v1/sessions/{id}/end", UUID.fromString(active.path("data").path("id").asText()))
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void concurrentConfirmOnlyCreatesOneActiveSession() throws Exception {
        String token = token(user("race-session@example.com", UserStatus.ACTIVE));
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-race");
        UUID qrId = UUID.fromString(qr.path("data").path("qrSessionId").asText());
        String nonce = qr.path("data").path("nonce").asText();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> first = () -> confirmStatus(qrId, nonce, token, "race-1");
            Callable<Integer> second = () -> confirmStatus(qrId, nonce, token, "race-2");
            List<Integer> statuses = executor.invokeAll(List.of(first, second)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();
            assertThat(statuses).contains(200);
            assertThat(sessionRepository.findByStation_IdOrderByStartedAtDesc(station.getId())).hasSize(1);
            assertThat(sessionRepository.findByStation_IdOrderByStartedAtDesc(station.getId()).getFirst().getStatus()).isEqualTo(SessionStatus.ACTIVE);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void inactiveAccountAndUnavailableStationAreRejected() throws Exception {
        AppUser inactive = user("inactive-session@example.com", UserStatus.ACTIVE);
        String inactiveToken = token(inactive);
        inactive = appUserRepository.findById(inactive.getId()).orElseThrow();
        inactive.setStatus(UserStatus.INACTIVE);
        appUserRepository.save(inactive);
        JsonNode qr = createQr(station.getId(), stationSecret, "qr-inactive");
        confirm(UUID.fromString(qr.path("data").path("qrSessionId").asText()), qr.path("data").path("nonce").asText(), inactiveToken, "confirm-inactive", 403);

        Station unavailable = stationRepository.findById(station.getId()).orElseThrow();
        unavailable.setStatus(StationStatus.MAINTENANCE);
        stationRepository.save(unavailable);
        mockMvc.perform(post("/api/v1/qr-sessions")
                        .header("X-Station-Id", station.getId().toString())
                        .header("X-Station-Secret", stationSecret)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    private JsonNode createQr(UUID stationId, String secret, String idempotencyKey) throws Exception {
        String response = mockMvc.perform(post("/api/v1/qr-sessions")
                        .header("X-Station-Id", stationId.toString())
                        .header("X-Station-Secret", secret)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode confirm(UUID qrId, String nonce, String token, String idempotencyKey, int expectedStatus) throws Exception {
        var result = mockMvc.perform(post("/api/v1/qr-sessions/{id}/confirm", qrId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nonce":"%s"}
                                """.formatted(nonce)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private int confirmStatus(UUID qrId, String nonce, String token, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/v1/qr-sessions/{id}/confirm", qrId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nonce":"%s"}
                                """.formatted(nonce)))
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

    private Branch branch(String code, String paymentPolicy) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(code + " Branch");
        branch.setStatus(BranchStatus.ACTIVE);
        branch.setTimezone("Asia/Ho_Chi_Minh");
        branch.setPaymentPolicy(paymentPolicy);
        branch.setOperatingStartTime(LocalTime.of(8, 0));
        branch.setOperatingEndTime(LocalTime.of(23, 0));
        return branchRepository.save(branch);
    }

    private Station station(Branch branch, String code, StationStatus status) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber("PC01".equals(code) ? 1 : 2);
        station.setStatus(status);
        return stationRepository.save(station);
    }

    private StationCredential credential(Station station, String rawSecret) {
        StationCredential credential = new StationCredential();
        credential.setStation(station);
        credential.setSecretHash(tokenHashService.hash(rawSecret));
        credential.setIssuedAt(Instant.now());
        return credentialRepository.save(credential);
    }

    private AppUser user(String email, UserStatus status) {
        Role role = roleRepository.findByCode(RoleCode.GAMER).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Gamer");
        user.setStatus(status);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }
}

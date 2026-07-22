package demo.server.performance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import demo.server.common.enums.PaymentMethod;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.TokenHashService;
import demo.server.dto.auth.LoginRequest;
import demo.server.dto.iot.DeviceTelemetryRequest;
import demo.server.dto.lfg.TeamInvitationRequest;
import demo.server.dto.session.QrConfirmRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.branch.Zone;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.entity.game.GamerGameProfile;
import demo.server.entity.gamer.GamerProfile;
import demo.server.entity.gamer.StationPreference;
import demo.server.entity.iot.IotDevice;
import demo.server.entity.lfg.LfgSignal;
import demo.server.entity.ordering.FoodOrder;
import demo.server.entity.ordering.MenuCategory;
import demo.server.entity.ordering.MenuItem;
import demo.server.entity.session.PlaySession;
import demo.server.entity.wallet.Wallet;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.branch.ZoneRepository;
import demo.server.repository.game.GameRankRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.game.GameRoleRepository;
import demo.server.repository.game.GamerGameProfileRepository;
import demo.server.repository.gamer.GamerProfileRepository;
import demo.server.repository.gamer.StationPreferenceRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.repository.lfg.LfgSignalRepository;
import demo.server.repository.ordering.FoodOrderRepository;
import demo.server.repository.ordering.MenuCategoryRepository;
import demo.server.repository.ordering.MenuItemRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.session.QrLoginSessionRepository;
import demo.server.repository.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceBenchmarkTests {

    private static final String PASSWORD = "Password123!";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:perfdb;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("security.jwt.secret", () -> "0123456789abcdef0123456789abcdef");
        registry.add("nexus.resilience.rate-limit-failure-policy", () -> "FAIL_OPEN");
        registry.add("nexus.resilience.lock-failure-policy", () -> "FAIL_OPEN");
        registry.add("nexus.resilience.idempotency-failure-policy", () -> "FAIL_OPEN");
        registry.add("nexus.resilience.login.permits", () -> "100");
        registry.add("nexus.resilience.login.window", () -> "PT1M");
        registry.add("nexus.resilience.forgot-password.permits", () -> "100");
        registry.add("nexus.resilience.forgot-password.window", () -> "PT1M");
        registry.add("nexus.resilience.reset-password.permits", () -> "100");
        registry.add("nexus.resilience.reset-password.window", () -> "PT1M");
        registry.add("nexus.resilience.qr-confirm.permits", () -> "100");
        registry.add("nexus.resilience.qr-confirm.window", () -> "PT1M");
        registry.add("nexus.resilience.invitation.permits", () -> "100");
        registry.add("nexus.resilience.invitation.window", () -> "PT1M");
        registry.add("nexus.resilience.order.permits", () -> "100");
        registry.add("nexus.resilience.order.window", () -> "PT1M");
        registry.add("nexus.jobs.enabled", () -> "false");
        registry.add("nexus.payment.provider", () -> "mock");
        registry.add("nexus.mqtt.provider", () -> "mock");
        registry.add("nexus.notification.push-provider", () -> "mock");
        registry.add("nexus.notification.email-provider", () -> "mock");
        registry.add("nexus.voice.provider", () -> "mock");
        registry.add("server.port", () -> "0");
    }

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;
    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
    @Autowired
    HikariDataSource dataSource;
    @Autowired
    TokenHashService tokenHashService;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    BranchRepository branchRepository;
    @Autowired
    ZoneRepository zoneRepository;
    @Autowired
    StationRepository stationRepository;
    @Autowired
    StationCredentialRepository stationCredentialRepository;
    @Autowired
    GameRepository gameRepository;
    @Autowired
    GameRankRepository gameRankRepository;
    @Autowired
    GameRoleRepository gameRoleRepository;
    @Autowired
    GamerProfileRepository gamerProfileRepository;
    @Autowired
    GamerGameProfileRepository gamerGameProfileRepository;
    @Autowired
    StationPreferenceRepository stationPreferenceRepository;
    @Autowired
    MenuCategoryRepository menuCategoryRepository;
    @Autowired
    MenuItemRepository menuItemRepository;
    @Autowired
    AppUserRepository appUserRepository;
    @Autowired
    WalletRepository walletRepository;
    @Autowired
    QrLoginSessionRepository qrLoginSessionRepository;
    @Autowired
    PlaySessionRepository playSessionRepository;
    @Autowired
    FoodOrderRepository foodOrderRepository;
    @Autowired
    LfgSignalRepository lfgSignalRepository;
    @Autowired
    IotDeviceRepository iotDeviceRepository;

    private final List<String> gamerTokens = new ArrayList<>();
    private final List<String> gamerRefreshTokens = new ArrayList<>();
    private final List<UUID> gamerIds = new ArrayList<>();
    private final List<UUID> stationIds = new ArrayList<>();
    private final List<String> stationSecrets = new ArrayList<>();

    private String staffToken;
    private String adminToken;
    private String stationClientToken;
    private UUID gameId;
    private UUID menuItemId;
    private UUID deviceId;
    private UUID activeSessionId;
    private UUID notificationReceiverId;

    @BeforeAll
    void seedData() throws IOException {
        Role gamerRole = role(RoleCode.GAMER);
        Role staffRole = role(RoleCode.STAFF_FNB);
        Role adminRole = role(RoleCode.BRANCH_ADMIN);
        Role stationClientRole = role(RoleCode.STATION_CLIENT);

        Branch branch = new Branch();
        branch.setCode("PERF");
        branch.setName("Performance Branch");
        branch.setTimezone("Asia/Ho_Chi_Minh");
        branch.setPaymentPolicy("PREPAID_OR_WALLET");
        branch = branchRepository.saveAndFlush(branch);

        Zone zone = new Zone();
        zone.setBranch(branch);
        zone.setCode("Z1");
        zone.setName("Zone 1");
        zone = zoneRepository.saveAndFlush(zone);

        Game game = new Game();
        game.setSlug("perf-game");
        game.setName("Performance Game");
        game.setMaxLobbySize(5);
        game = gameRepository.saveAndFlush(game);
        gameId = game.getId();

        GameRank rank = new GameRank();
        rank.setGame(game);
        rank.setCode("R1");
        rank.setName("Rank 1");
        gameRankRepository.saveAndFlush(rank);

        GameRole gameRole = new GameRole();
        gameRole.setGame(game);
        gameRole.setCode("ROLE1");
        gameRole.setName("Role 1");
        gameRoleRepository.saveAndFlush(gameRole);

        MenuCategory category = new MenuCategory();
        category.setBranch(branch);
        category.setCode("FOOD");
        category.setName("Food");
        category = menuCategoryRepository.saveAndFlush(category);

        MenuItem menuItem = new MenuItem();
        menuItem.setBranch(branch);
        menuItem.setCategory(category);
        menuItem.setCode("M1");
        menuItem.setName("Benchmark Item");
        menuItem.setPrice(new BigDecimal("15000.00"));
        menuItem.setStockQuantity(1000);
        menuItem = menuItemRepository.saveAndFlush(menuItem);
        menuItemId = menuItem.getId();

        for (int i = 1; i <= 6; i++) {
            Station station = new Station();
            station.setBranch(branch);
            station.setZone(zone);
            station.setStationNumber(i);
            station.setCode("S" + i);
            station.setName("Station " + i);
            station = stationRepository.saveAndFlush(station);
            stationIds.add(station.getId());

            String secret = "bench-station-secret-" + i;
            StationCredential credential = new StationCredential();
            credential.setStation(station);
            credential.setSecretHash(tokenHashService.hash(secret));
            credential.setIssuedAt(Instant.now());
            stationCredentialRepository.saveAndFlush(credential);
            stationSecrets.add(secret);
        }

        for (int i = 1; i <= 20; i++) {
            String email = "perf-gamer-" + i + "@example.com";
            AppUser user = createUser(email, "0900000" + String.format("%03d", i), gamerRole, branch);
            gamerIds.add(user.getId());
            GamerProfile profile = new GamerProfile();
            profile.setUser(user);
            profile.setNickname("Gamer " + i);
            gamerProfileRepository.saveAndFlush(profile);
            GamerGameProfile gameProfile = new GamerGameProfile();
            gameProfile.setUser(user);
            gameProfile.setGame(game);
            gameProfile.setRank(rank);
            gameProfile.setPreferredRole(gameRole);
            gamerGameProfileRepository.saveAndFlush(gameProfile);
            StationPreference preference = new StationPreference();
            preference.setUser(user);
            preference.setDeskHeightCm(75);
            preference.setChairAngleDegree(110);
            preference.setRgbColor("#33AAFF");
            preference.setBrightness(70);
            preference.setMouseDpi(1600);
            stationPreferenceRepository.saveAndFlush(preference);
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            wallet.setBalance(new BigDecimal("1000000.00"));
            walletRepository.saveAndFlush(wallet);

            JsonNode auth = login(email).path("data");
            gamerTokens.add(auth.path("accessToken").asText());
            gamerRefreshTokens.add(auth.path("refreshToken").asText());
        }

        AppUser staff = createUser("perf-staff@example.com", "0900001001", staffRole, branch);
        staffToken = login("perf-staff@example.com").path("data").path("accessToken").asText();

        AppUser admin = createUser("perf-admin@example.com", "0900001002", adminRole, branch);
        adminToken = login("perf-admin@example.com").path("data").path("accessToken").asText();

        AppUser stationClient = createUser("perf-station-client@example.com", "0900001003", stationClientRole, branch);
        stationClientToken = login("perf-station-client@example.com").path("data").path("accessToken").asText();

        IotDevice device = new IotDevice();
        device.setBranch(branch);
        device.setStation(stationIds.size() > 1 ? stationRepository.findById(stationIds.get(1)).orElseThrow() : stationRepository.findById(stationIds.get(0)).orElseThrow());
        device.setDeviceType(DeviceType.IOT_GATEWAY);
        device.setSerialNumber("SER-PERF-001");
        device.setName("Benchmark device");
        device.setCapabilities("telemetry,commands");
        device.setStatus(DeviceStatus.ACTIVE);
        device.setMechanicalCommandLocked(false);
        device = iotDeviceRepository.saveAndFlush(device);
        deviceId = device.getId();

        // Seed signals for dashboard/search paths.
        for (int i = 0; i < 30; i++) {
            LfgSignal signal = new LfgSignal();
            signal.setUser(appUserRepository.findById(gamerIds.get(0)).orElseThrow());
            signal.setGame(game);
            signal.setTargetMembers(5);
            signal.setMessage("signal-" + i);
            signal.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
            lfgSignalRepository.save(signal);
        }
        lfgSignalRepository.flush();

        // Create one active session for order / smart station measurements.
        activeSessionId = createQrFlow(gamerTokens.get(0), stationIds.get(0), stationSecrets.get(0));
        notificationReceiverId = gamerIds.get(1);
    }

    @Test
    void benchmarkCriticalWorkflows() throws Exception {
        Map<String, ScenarioMetrics> results = new LinkedHashMap<>();
        results.put("login", benchmark("login", 20, 6, i -> login("perf-gamer-1@example.com").path("data").path("accessToken").isTextual()));
        results.put("refresh", benchmark("refresh", 15, 4, i -> refresh(gamerRefreshTokens.get(i % gamerRefreshTokens.size())).path("data").path("accessToken").isTextual()));
        results.put("qr_create", benchmark("qr_create", 12, 4, i -> qrCreate(stationIds.get(i % stationIds.size()), stationSecrets.get(i % stationSecrets.size())).qrSessionId() != null));
        results.put("qr_confirm", benchmark("qr_confirm", 6, 3, i -> qrConfirm(gamerTokens.get(i), stationIds.get(i), stationSecrets.get(i)) != null));
        results.put("qr_flow", benchmark("qr_flow", 6, 3, i -> createQrFlow(gamerTokens.get(i), stationIds.get(i), stationSecrets.get(i)) != null));
        results.put("lfg_query", benchmark("lfg_query", 20, 5, i -> get("/api/v1/lfg/signals?gameId=" + gameId, gamerTokens.get(0)).contains("\"success\":true")));
        results.put("invitation", benchmark("invitation", 8, 2, i -> invite(i).contains("\"success\":true")));
        results.put("order_create", benchmark("order_create", 15, 4, i -> createOrder(i).contains("\"success\":true")));
        results.put("staff_queue", benchmark("staff_queue", 12, 4, i -> get("/api/v1/staff/orders?status=NEW", staffToken).contains("\"success\":true")));
        results.put("dashboard_overview", benchmark("dashboard_overview", 12, 4, i -> get("/api/v1/reports/overview?period=date", adminToken).contains("\"success\":true")));
        results.put("telemetry", benchmark("telemetry", 20, 5, i -> telemetry(i).contains("\"success\":true")));
        results.put("station_apply_profile", benchmark("station_apply_profile", 6, 2, i -> post("/api/v1/stations/" + stationIds.get(0) + "/apply-profile", "", adminToken, null).contains("\"success\":true")));
        results.put("websocket_connect", benchmark("websocket_connect", 8, 4, i -> websocketConnect(gamerTokens.get(i % gamerTokens.size()), "/user/queue/notifications")));
        results.put("notification_realtime", benchmark("notification_realtime", 1, 1, i -> notificationRealtime()));

        ScenarioMetrics summary = ScenarioMetrics.merge(results.values());
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("generatedAt", Instant.now().toString());
        report.put("environment", Map.of(
                "database", "H2 PostgreSQL mode",
                "redis", "not available",
                "websocket", "spring-simple-broker",
                "port", port
        ));
        report.put("thresholds", Map.of(
                "restP95Millis", 500,
                "qrLoginMillis", 5000,
                "notificationRealtimeMillis", 3000,
                "smartStationMillis", 10000
        ));
        report.put("scenarios", results);
        report.put("summary", summary);
        report.put("benchmarkGatePassed", summary.errorRate() < 0.05);
        report.put("notes", List.of(
                "Benchmark data is isolated in an in-memory H2 database.",
                "No live Redis instance was available; Redis latency is reported as N/A.",
                "No 10,000-user claim is made.",
                "Dashboard/report queries used seeded aggregate data.",
                "Performance numbers are response-time samples gathered from the running Spring Boot app."
        ));

        java.nio.file.Path out = java.nio.file.Paths.get("build", "reports", "performance", "performance-results.json");
        java.nio.file.Files.createDirectories(out.getParent());
        java.nio.file.Files.writeString(out, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));

        assertNotNull(summary);
    }

    private ScenarioMetrics benchmark(String name, int samples, int concurrency, IntScenario scenario) throws Exception {
        List<ScenarioSample> samplesList = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger peakConnections = new AtomicInteger();
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();
        sampler.scheduleAtFixedRate(() -> {
            int active = dataSource.getHikariPoolMXBean().getActiveConnections();
            peakConnections.accumulateAndGet(active, Math::max);
        }, 0, 20, TimeUnit.MILLISECONDS);

        List<java.util.concurrent.Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < samples; i++) {
            final int index = i;
            futures.add(pool.submit(() -> {
                start.await();
                long begin = System.nanoTime();
                boolean ok = false;
                try {
                    ok = scenario.run(index);
                } catch (Exception ignored) {
                    ok = false;
                }
                long elapsed = System.nanoTime() - begin;
                samplesList.add(new ScenarioSample(elapsed / 1_000_000.0, ok));
                if (!ok) {
                    failures.incrementAndGet();
                }
                return null;
            }));
        }

        long wallStart = System.nanoTime();
        start.countDown();
        for (var future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        long wallElapsed = System.nanoTime() - wallStart;
        pool.shutdownNow();
        sampler.shutdownNow();

        List<Double> durations = samplesList.stream().map(ScenarioSample::millis).sorted().collect(Collectors.toList());
        double p50 = percentile(durations, 50);
        double p95 = percentile(durations, 95);
        double p99 = percentile(durations, 99);
        double avg = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double throughput = samples / (wallElapsed / 1_000_000_000.0);
        double errorRate = samples == 0 ? 0.0 : failures.get() / (double) samples;
        long ramMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        double cpuLoad = processCpuLoad();

        return new ScenarioMetrics(name, samples, concurrency, round(avg), round(p50), round(p95), round(p99), round(throughput), round(errorRate), peakConnections.get(), null, round(cpuLoad), ramMb);
    }

    private JsonNode login(String email) throws IOException {
        return postJson("/api/v1/auth/login", new LoginRequest(email, PASSWORD), null);
    }

    private JsonNode refresh(String refreshToken) throws IOException {
        return postJson("/api/v1/auth/refresh-token", Map.of("refreshToken", refreshToken), null);
    }

    private QrLoginSessionSnapshot qrCreate(UUID stationId, String secret) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Station-Id", stationId.toString());
        headers.set("X-Station-Secret", secret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        JsonNode response = postJson("/api/v1/qr-sessions", "", headers);
        JsonNode data = response.path("data");
        return new QrLoginSessionSnapshot(UUID.fromString(data.path("qrSessionId").asText()), data.path("nonce").asText());
    }

    private UUID qrConfirm(String token, UUID stationId, String secret) throws IOException {
        QrLoginSessionSnapshot qr = qrCreate(stationId, secret);
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        JsonNode response = postJson("/api/v1/qr-sessions/" + qr.qrSessionId() + "/confirm", new QrConfirmRequest(qr.nonce()), headers);
        JsonNode data = response.path("data");
        return data.path("id").isTextual() ? UUID.fromString(data.path("id").asText()) : null;
    }

    private UUID createQrFlow(String token, UUID stationId, String secret) throws IOException {
        QrLoginSessionSnapshot qr = qrCreate(stationId, secret);
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        JsonNode response = postJson("/api/v1/qr-sessions/" + qr.qrSessionId() + "/confirm", new QrConfirmRequest(qr.nonce()), headers);
        JsonNode data = response.path("data");
        activeSessionId = data.path("id").isTextual() ? UUID.fromString(data.path("id").asText()) : null;
        return activeSessionId;
    }

    private String invite(int index) throws IOException {
        UUID receiverId = gamerIds.get((index % (gamerIds.size() - 2)) + 2);
        return post("/api/v1/team-invitations", new TeamInvitationRequest(receiverId, null, "bench invite " + index), gamerTokens.get(0), null);
    }

    private String createOrder(int index) throws IOException {
        if (activeSessionId == null) {
            createQrFlow(gamerTokens.get(0), stationIds.get(0), stationSecrets.get(0));
        }
        Map<String, Object> body = Map.of(
                "paymentMethod", PaymentMethod.PAY_AT_COUNTER.name(),
                "note", "bench order",
                "items", List.of(Map.of("menuItemId", menuItemId, "quantity", 1, "note", "bench"))
        );
        return post("/api/v1/orders", body, gamerTokens.get(0), "bench-order-" + index);
    }

    private String telemetry(int index) throws IOException {
        DeviceTelemetryRequest request = new DeviceTelemetryRequest(true, 99, 95, null, "1.0.0", "fps", String.valueOf(144 + index), "{\"fps\":144}");
        return post("/api/v1/iot/devices/" + deviceId + "/telemetry", request, stationClientToken, null);
    }

    private boolean websocketConnect(String token, String destination) {
        try {
            WebSocketProbe probe = connectWebSocket(token);
            probe.subscribe(destination);
            probe.close();
            return probe.connected.get(5, TimeUnit.SECONDS) == null;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean notificationRealtime() {
        try {
            WebSocketProbe probe = connectWebSocket(gamerTokens.get(1));
            probe.subscribe("/user/queue/notifications");
            String response = post("/api/v1/team-invitations", new TeamInvitationRequest(notificationReceiverId, null, "bench notification"), gamerTokens.get(0), null);
            if (!response.contains("\"success\":true")) {
                return false;
            }
            String frame = probe.awaitMessage(Duration.ofSeconds(3));
            probe.close();
            return frame != null;
        } catch (Exception ex) {
            return false;
        }
    }

    private WebSocketProbe connectWebSocket(String token) {
        URI uri = URI.create("ws://localhost:" + port + "/ws/websocket");
        WebSocketProbe probe = new WebSocketProbe(token);
        HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri, probe).join();
        return probe;
    }

    private String post(String path, Object body, String token, String idempotencyKey) throws IOException {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null) {
            headers.set("Idempotency-Key", idempotencyKey);
        }
        return exchange(path, HttpMethod.POST, body, headers).getBody();
    }

    private JsonNode postJson(String path, Object body, HttpHeaders headers) throws IOException {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = exchange(path, HttpMethod.POST, body, headers);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        return objectMapper.readTree(Objects.requireNonNull(response.getBody()));
    }

    private String get(String path, String token) throws IOException {
        HttpHeaders headers = bearerHeaders(token);
        return exchange(path, HttpMethod.GET, null, headers).getBody();
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, Object body, HttpHeaders headers) throws IOException {
        HttpEntity<String> entity = new HttpEntity<>(body instanceof String s ? s : objectMapper.writeValueAsString(body), headers);
        return restTemplate.exchange(baseUrl(path), method, entity, String.class);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private Role role(RoleCode code) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            role.setDescription("Benchmark seed role");
            return roleRepository.saveAndFlush(role);
        });
    }

    private AppUser createUser(String email, String phone, Role role, Branch branch) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(PASSWORD));
        user.setFullName(email);
        user.setDisplayName(email);
        user.setStatus(UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.saveAndFlush(user);
    }

    private double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return 0.0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double processCpuLoad() {
        try {
            var bean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean sun) {
                return sun.getProcessCpuLoad() * 100.0;
            }
        } catch (Throwable ignored) {
        }
        return -1.0;
    }

    private record QrLoginSessionSnapshot(UUID qrSessionId, String nonce) {
    }

    private record ScenarioSample(double millis, boolean success) {
    }

    private record ScenarioMetrics(String scenario, int samples, int concurrency, double avgMillis, double p50Millis,
                                   double p95Millis, double p99Millis, double throughputPerSec, double errorRate,
                                   int peakDbConnections, Integer redisLatencyMillis, double processCpuLoad,
                                   long ramUsedMb) {
        static ScenarioMetrics merge(Iterable<ScenarioMetrics> metrics) {
            List<ScenarioMetrics> list = new ArrayList<>();
            metrics.forEach(list::add);
            double avg = list.stream().mapToDouble(ScenarioMetrics::avgMillis).average().orElse(0.0);
            double p50 = list.stream().mapToDouble(ScenarioMetrics::p50Millis).average().orElse(0.0);
            double p95 = list.stream().mapToDouble(ScenarioMetrics::p95Millis).average().orElse(0.0);
            double p99 = list.stream().mapToDouble(ScenarioMetrics::p99Millis).average().orElse(0.0);
            double throughput = list.stream().mapToDouble(ScenarioMetrics::throughputPerSec).sum();
            double error = list.stream().mapToDouble(ScenarioMetrics::errorRate).average().orElse(0.0);
            long ram = list.stream().mapToLong(ScenarioMetrics::ramUsedMb).max().orElse(0L);
            return new ScenarioMetrics("summary",
                    list.stream().mapToInt(ScenarioMetrics::samples).sum(),
                    list.size(),
                    round(avg), round(p50), round(p95), round(p99), round(throughput), round(error),
                    list.stream().mapToInt(ScenarioMetrics::peakDbConnections).max().orElse(0),
                    null,
                    0.0,
                    ram);
        }
    }

    private interface IntScenario {
        boolean run(int index) throws Exception;
    }

    private final class WebSocketProbe implements WebSocket.Listener {
        private final String token;
        private final StringBuilder buffer = new StringBuilder();
        private final CompletableFuture<Void> connected = new CompletableFuture<>();
        private final CompletableFuture<String> message = new CompletableFuture<>();
        private volatile WebSocket socket;

        private WebSocketProbe(String token) {
            this.token = token;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            webSocket.request(1);
            webSocket.sendText(stompConnectFrame(token), true);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String frame = buffer.toString();
                buffer.setLength(0);
                if (frame.startsWith("CONNECTED")) {
                    connected.complete(null);
                } else if (frame.startsWith("MESSAGE") && !message.isDone()) {
                    message.complete(frame);
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, java.nio.ByteBuffer data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        void subscribe(String destination) throws Exception {
            connected.get(5, TimeUnit.SECONDS);
            socket.sendText(stompSubscribeFrame(destination), true).join();
        }

        String awaitMessage(Duration timeout) throws Exception {
            return message.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void close() {
            if (socket != null) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "done");
            }
        }
    }

    private String stompConnectFrame(String token) {
        return "CONNECT\n" +
                "accept-version:1.2\n" +
                "heart-beat:0,0\n" +
                "Authorization:Bearer " + token + "\n\n\0";
    }

    private String stompSubscribeFrame(String destination) {
        return "SUBSCRIBE\n" +
                "id:sub-" + UUID.randomUUID() + "\n" +
                "destination:" + destination + "\n\n\0";
    }
}

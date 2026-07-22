package demo.server.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.DeviceCommandStatus;
import demo.server.common.enums.DeviceCommandType;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.security.TokenHashService;
import demo.server.dto.iot.command.DeviceCommandAckRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.gamer.StationPreference;
import demo.server.entity.iot.DeviceCommand;
import demo.server.entity.iot.IotDevice;
import demo.server.entity.session.PlaySession;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.gamer.StationPreferenceRepository;
import demo.server.repository.iot.CommandHistoryRepository;
import demo.server.repository.iot.DeviceAlertRepository;
import demo.server.repository.iot.DeviceCommandRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.repository.session.PlaySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DeviceCommandMqttTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    BranchRepository branchRepository;

    @Autowired
    StationRepository stationRepository;

    @Autowired
    StationCredentialRepository credentialRepository;

    @Autowired
    StationPreferenceRepository preferenceRepository;

    @Autowired
    PlaySessionRepository sessionRepository;

    @Autowired
    IotDeviceRepository deviceRepository;

    @Autowired
    DeviceCommandRepository commandRepository;

    @Autowired
    CommandHistoryRepository historyRepository;

    @Autowired
    DeviceAlertRepository alertRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    TokenHashService tokenHashService;

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
    }

    @Test
    void applyPreferencePublishesCommandsAndAggregatesSuccess() throws Exception {
        TestContext ctx = context("CMD01");
        createCompatibleDevices(ctx.branch, ctx.station);
        preference(ctx.gamer, 80, 115, "#00FFAA", 65, 1600, true);
        String token = token(ctx.gamer);

        JsonNode response = postJson("/api/v1/iot/stations/" + ctx.station.getId() + "/apply-profile", token, "");

        assertThat(response.path("data").path("status").asText()).isEqualTo("SUCCESS");
        assertThat(response.path("data").path("commands")).hasSize(6);
        assertThat(commandRepository.findAll()).hasSize(6);
        assertThat(commandRepository.findAll()).allMatch(command -> command.getStatus() == DeviceCommandStatus.SUCCESS);
    }

    @Test
    void missingCompatibleDevicesProducePartialSuccessWithoutBlockingSession() throws Exception {
        TestContext ctx = context("CMD02");
        device(ctx.branch, ctx.station, DeviceType.SMART_DESK, "CMD02-DESK");
        preference(ctx.gamer, 75, 110, "#FFFFFF", 70, 1600, false);

        JsonNode response = postJson("/api/v1/iot/stations/" + ctx.station.getId() + "/apply-profile", token(ctx.gamer), "");

        assertThat(response.path("data").path("status").asText()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(sessionRepository.findById(ctx.session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(commandRepository.findAll()).anyMatch(command -> command.getStatus() == DeviceCommandStatus.SKIPPED);
    }

    @Test
    void timeoutRetriesSafeCommandsButNotDangerousCommandsBlindly() throws Exception {
        TestContext ctx = context("CMD03");
        IotDevice mouse = device(ctx.branch, ctx.station, DeviceType.MOUSE, "CMD03-MOUSE");
        DeviceCommand safe = command(ctx, mouse, DeviceCommandType.MOUSE_DPI, "TIMEOUT", "dpi", false, 1, 3);
        safe.setStatus(DeviceCommandStatus.SENT);
        safe.setSentAt(Instant.now().minusSeconds(60));
        commandRepository.save(safe);
        IotDevice desk = device(ctx.branch, ctx.station, DeviceType.SMART_DESK, "CMD03-DESK");
        DeviceCommand dangerous = command(ctx, desk, DeviceCommandType.DESK_HEIGHT_CM, "80", "cm", true, 1, 1);
        dangerous.setStatus(DeviceCommandStatus.SENT);
        dangerous.setSentAt(Instant.now().minusSeconds(60));
        commandRepository.save(dangerous);
        String adminToken = token(ctx.admin);

        JsonNode first = postJson("/api/v1/admin/iot/commands/timeouts", adminToken, "");
        assertThat(first.path("data")).hasSize(2);
        assertThat(commandRepository.findById(safe.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.SENT);
        assertThat(commandRepository.findById(safe.getId()).orElseThrow().getAttemptCount()).isEqualTo(2);
        assertThat(commandRepository.findById(dangerous.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.TIMEOUT);

        DeviceCommand retry = commandRepository.findById(safe.getId()).orElseThrow();
        retry.setSentAt(Instant.now().minusSeconds(60));
        retry.setAttemptCount(3);
        commandRepository.save(retry);
        postJson("/api/v1/admin/iot/commands/timeouts", adminToken, "");
        assertThat(commandRepository.findById(safe.getId()).orElseThrow().getStatus()).isEqualTo(DeviceCommandStatus.TIMEOUT);
    }

    @Test
    void duplicateAckIsIdempotentAndFakeOrCrossBranchAckIsRejected() throws Exception {
        TestContext ctx = context("CMD04");
        IotDevice mouse = device(ctx.branch, ctx.station, DeviceType.MOUSE, "CMD04-MOUSE");
        DeviceCommand command = command(ctx, mouse, DeviceCommandType.MOUSE_DPI, "1600", "dpi", false, 1, 3);
        command.setStatus(DeviceCommandStatus.SENT);
        command.setSentAt(Instant.now());
        commandRepository.save(command);
        String secret = credential(ctx.station);
        DeviceCommandAckRequest ack = new DeviceCommandAckRequest(ctx.branch.getId(), ctx.station.getId(), mouse.getId(), command.getCorrelationId(), true, "ok");

        postAck(secret, ack).andExpect(jsonPath("$.data.status").value("SUCCESS"));
        postAck(secret, ack).andExpect(jsonPath("$.data.status").value("SUCCESS"));
        assertThat(historyRepository.findByCommandIdOrderByCreatedAtAsc(command.getId()).stream()
                .filter(history -> "ACK".equals(history.getAction()))).hasSize(1);

        mockMvc.perform(post("/api/v1/iot/commands/ack")
                        .header("X-Station-Secret", "fake-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ack)))
                .andExpect(status().isUnauthorized());

        Branch other = branch("CMD04X");
        DeviceCommandAckRequest spoofed = new DeviceCommandAckRequest(other.getId(), ctx.station.getId(), mouse.getId(), command.getCorrelationId(), true, "spoof");
        postAck(secret, spoofed).andExpect(status().isForbidden());
    }

    @Test
    void criticalEmergencyStopCreatesAlertAndLocksMechanicalCommands() throws Exception {
        TestContext ctx = context("CMD05");
        IotDevice desk = device(ctx.branch, ctx.station, DeviceType.SMART_DESK, "CMD05-DESK");

        mockMvc.perform(post("/api/v1/admin/devices/{deviceId}/emergency-stop", desk.getId())
                        .header("Authorization", "Bearer " + token(ctx.admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commandType").value("EMERGENCY_STOP"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        assertThat(alertRepository.findAll()).hasSize(1);
        assertThat(deviceRepository.findById(desk.getId()).orElseThrow().isMechanicalCommandLocked()).isTrue();
    }

    private TestContext context(String code) {
        Branch branch = branch(code);
        Station station = station(branch, "PC01");
        AppUser gamer = user(code.toLowerCase() + "-gamer@example.com", RoleCode.GAMER, branch);
        AppUser admin = user(code.toLowerCase() + "-admin@example.com", RoleCode.BRANCH_ADMIN, branch);
        PlaySession session = new PlaySession();
        session.setUser(gamer);
        session.setStation(station);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        sessionRepository.save(session);
        return new TestContext(branch, station, gamer, admin, session);
    }

    private void createCompatibleDevices(Branch branch, Station station) {
        device(branch, station, DeviceType.SMART_DESK, branch.getCode() + "-DESK");
        device(branch, station, DeviceType.GAMING_CHAIR, branch.getCode() + "-CHAIR");
        device(branch, station, DeviceType.RGB_LIGHTING, branch.getCode() + "-RGB");
        device(branch, station, DeviceType.MOUSE, branch.getCode() + "-MOUSE");
    }

    private IotDevice device(Branch branch, Station station, DeviceType type, String serial) {
        IotDevice device = new IotDevice();
        device.setBranch(branch);
        device.setStation(station);
        device.setDeviceType(type);
        device.setSerialNumber(serial);
        device.setStatus(DeviceStatus.ONLINE);
        device.setName(type.name());
        return deviceRepository.save(device);
    }

    private DeviceCommand command(TestContext ctx, IotDevice device, DeviceCommandType type, String value, String unit,
                                  boolean dangerous, int attempts, int maxAttempts) {
        DeviceCommand command = new DeviceCommand();
        command.setBranch(ctx.branch);
        command.setStation(ctx.station);
        command.setDevice(device);
        command.setUser(ctx.gamer);
        command.setPlaySession(ctx.session);
        command.setCorrelationId(UUID.randomUUID());
        command.setCommandType(type);
        command.setCommandValue(value);
        command.setUnit(unit);
        command.setDangerous(dangerous);
        command.setAttemptCount(attempts);
        command.setMaxAttempts(maxAttempts);
        command.setMqttTopic("nexus/%s/%s/%s/command".formatted(ctx.branch.getId(), ctx.station.getId(), device.getId()));
        return commandRepository.save(command);
    }

    private void preference(AppUser user, Integer desk, Integer chair, String color, Integer brightness, Integer dpi, boolean nightMode) {
        StationPreference preference = new StationPreference();
        preference.setUser(user);
        preference.setDeskHeightCm(desk);
        preference.setChairAngleDegree(chair);
        preference.setRgbColor(color);
        preference.setBrightness(brightness);
        preference.setMouseDpi(dpi);
        preference.setNightMode(nightMode);
        preferenceRepository.save(preference);
    }

    private String credential(Station station) {
        String secret = "StationSecret-" + station.getCode();
        StationCredential credential = new StationCredential();
        credential.setStation(station);
        credential.setSecretHash(tokenHashService.hash(secret));
        credential.setIssuedAt(Instant.now());
        credentialRepository.save(credential);
        return secret;
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(code + " Branch");
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
        station.setStationNumber(1);
        station.setStatus(StationStatus.AVAILABLE);
        return stationRepository.save(station);
    }

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Command User");
        user.setStatus(demo.server.common.enums.UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private String token(AppUser user) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"Password123"}
                                """.formatted(user.getEmail())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private JsonNode postJson(String path, String bearerToken, Object body) throws Exception {
        String json = body instanceof String text ? text : objectMapper.writeValueAsString(body);
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.ResultActions postAck(String secret, DeviceCommandAckRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/iot/commands/ack")
                .header("X-Station-Secret", secret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private record TestContext(Branch branch, Station station, AppUser gamer, AppUser admin, PlaySession session) {
    }
}

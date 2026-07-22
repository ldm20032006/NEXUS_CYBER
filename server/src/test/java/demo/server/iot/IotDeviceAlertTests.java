package demo.server.iot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.AlertSeverity;
import demo.server.common.enums.AlertStatus;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.StationStatus;
import demo.server.dto.iot.AssignDeviceAlertRequest;
import demo.server.dto.iot.DeviceAlertRequest;
import demo.server.dto.iot.DeviceAlertStatusRequest;
import demo.server.dto.iot.DeviceHeartbeatRequest;
import demo.server.dto.iot.DeviceTelemetryRequest;
import demo.server.dto.iot.IotDeviceRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.iot.AlertHistoryRepository;
import demo.server.repository.iot.DeviceAlertRepository;
import demo.server.repository.iot.DeviceTelemetryRepository;
import demo.server.repository.iot.IotDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IotDeviceAlertTests {

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
    IotDeviceRepository deviceRepository;

    @Autowired
    DeviceAlertRepository alertRepository;

    @Autowired
    AlertHistoryRepository historyRepository;

    @Autowired
    DeviceTelemetryRepository telemetryRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

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
    void heartbeatResetsMissesAndThirdMissMarksOfflineWithSingleAlert() throws Exception {
        Branch branch = branch("IOT01");
        Station station = station(branch, "PC01", 1);
        String adminToken = token(user("branch-hb@example.com", RoleCode.BRANCH_ADMIN, branch));
        String token = token(user("tech@example.com", RoleCode.STAFF_TECHNICAL, branch));
        UUID deviceId = createDevice(adminToken, branch.getId(), station.getId(), "SER-HB-1");

        mockMvc.perform(post("/api/v1/iot/devices/{id}/heartbeat", deviceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeviceHeartbeatRequest("1.0.1", "10.0.0.10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONLINE"))
                .andExpect(jsonPath("$.data.missedHeartbeatCount").value(0))
                .andExpect(jsonPath("$.data.lastHeartbeatAt").exists());

        missHeartbeat(token, deviceId).andExpect(jsonPath("$.data.status").value("ONLINE"));
        missHeartbeat(token, deviceId).andExpect(jsonPath("$.data.status").value("ONLINE"));
        missHeartbeat(token, deviceId)
                .andExpect(jsonPath("$.data.status").value("OFFLINE"))
                .andExpect(jsonPath("$.data.missedHeartbeatCount").value(3));

        missHeartbeat(token, deviceId).andExpect(jsonPath("$.data.status").value("OFFLINE"));
        assertThat(alertRepository.findAll()).hasSize(1);
        assertThat(alertRepository.findAll().getFirst().getAlertCode()).isEqualTo("HEARTBEAT_MISSED");
    }

    @Test
    void duplicateOpenAlertIsRejectedAndCriticalLocksMechanicalCommands() throws Exception {
        Branch branch = branch("IOT02");
        Station station = station(branch, "PC01", 1);
        String token = token(user("admin-alert@example.com", RoleCode.BRANCH_ADMIN, branch));
        UUID deviceId = createDevice(token, branch.getId(), station.getId(), "SER-ALERT-1");

        DeviceAlertRequest request = new DeviceAlertRequest(deviceId, "motor-jam", "Motor jam", "Desk motor jammed", AlertSeverity.CRITICAL, "Check motor");
        JsonNode alert = postJson("/api/v1/staff/device-alerts", token, request);
        UUID alertId = UUID.fromString(alert.path("data").path("id").asText());

        assertThat(deviceRepository.findById(deviceId).orElseThrow().isMechanicalCommandLocked()).isTrue();
        assertThat(alertRepository.findById(alertId).orElseThrow().isCriticalMechanicalLock()).isTrue();

        mockMvc.perform(post("/api/v1/staff/device-alerts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void alertAssignTransitionAndHistoryAreScopedAndAppendOnly() throws Exception {
        Branch branch = branch("IOT03");
        Station station = station(branch, "PC01", 1);
        String adminToken = token(user("branch-alert@example.com", RoleCode.BRANCH_ADMIN, branch));
        AppUser staff = user("staff-alert@example.com", RoleCode.STAFF_TECHNICAL, branch);
        UUID deviceId = createDevice(adminToken, branch.getId(), station.getId(), "SER-FLOW-1");
        UUID alertId = UUID.fromString(postJson("/api/v1/staff/device-alerts", adminToken,
                new DeviceAlertRequest(deviceId, "TEMP_HIGH", "Temperature high", "Thermal sensor high", AlertSeverity.HIGH, null))
                .path("data").path("id").asText());

        patchJson("/api/v1/staff/device-alerts/" + alertId + "/assign", adminToken, new AssignDeviceAlertRequest(staff.getId(), "Take ownership"))
                .andExpect(jsonPath("$.data.assignedStaffId").value(staff.getId().toString()));
        patchJson("/api/v1/staff/device-alerts/" + alertId, adminToken, new DeviceAlertStatusRequest(AlertStatus.ACKNOWLEDGED, "Ack"))
                .andExpect(jsonPath("$.data.status").value("ACKNOWLEDGED"));
        patchJson("/api/v1/staff/device-alerts/" + alertId, adminToken, new DeviceAlertStatusRequest(AlertStatus.IN_PROGRESS, "Working"))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        patchJson("/api/v1/staff/device-alerts/" + alertId, adminToken, new DeviceAlertStatusRequest(AlertStatus.RESOLVED, "Resolved"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
        patchJson("/api/v1/staff/device-alerts/" + alertId, adminToken, new DeviceAlertStatusRequest(AlertStatus.CLOSED, "Closed"))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        mockMvc.perform(patch("/api/v1/staff/device-alerts/{id}", alertId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DeviceAlertStatusRequest(AlertStatus.IN_PROGRESS, "Invalid"))))
                .andExpect(status().isUnprocessableEntity());

        assertThat(historyRepository.findByAlertIdOrderByCreatedAtAsc(alertId)).hasSize(6);
        JsonNode history = getJson("/api/v1/staff/device-alerts/" + alertId + "/history", adminToken);
        assertThat(history.path("data")).hasSize(6);
    }

    @Test
    void branchScopeAndTelemetryRangeAreEnforced() throws Exception {
        Branch own = branch("IOT04");
        Branch other = branch("IOT05");
        Station ownStation = station(own, "PC01", 1);
        Station otherStation = station(other, "PC02", 1);
        String ownToken = token(user("branch-scope@example.com", RoleCode.BRANCH_ADMIN, own));
        String superToken = token(user("super-iot@example.com", RoleCode.SUPER_ADMIN, null));
        UUID ownDeviceId = createDevice(ownToken, own.getId(), ownStation.getId(), "SER-SCOPE-1");
        UUID otherDeviceId = createDevice(superToken, other.getId(), otherStation.getId(), "SER-SCOPE-2");

        mockMvc.perform(get("/api/v1/admin/devices/{id}", otherDeviceId)
                        .header("Authorization", "Bearer " + ownToken))
                .andExpect(status().isForbidden());

        postJson("/api/v1/iot/devices/" + ownDeviceId + "/telemetry", ownToken,
                new DeviceTelemetryRequest(true, 90, 88, null, "1.0.0", "temperature", "35.5", "{\"unit\":\"c\"}"));

        JsonNode telemetry = getJson("/api/v1/iot/devices/" + ownDeviceId + "/telemetry?limit=1&size=20", ownToken);
        assertThat(telemetry.path("data").path("content")).hasSize(1);
        assertThat(telemetryRepository.findAll()).hasSize(1);

        mockMvc.perform(post("/api/v1/admin/devices")
                        .header("Authorization", "Bearer " + ownToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IotDeviceRequest(own.getId(), otherStation.getId(),
                                DeviceType.IOT_GATEWAY, "SER-CROSS", "Cross", "1", "{}", DeviceStatus.ACTIVE, null))))
                .andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions missHeartbeat(String token, UUID deviceId) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/devices/{id}/missed-heartbeat", deviceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private UUID createDevice(String token, UUID branchId, UUID stationId, String serial) throws Exception {
        JsonNode response = postJson("/api/v1/admin/devices", token, new IotDeviceRequest(branchId, stationId, DeviceType.IOT_GATEWAY,
                serial, "Gateway", "1.0.0", "{\"heartbeat\":true}", DeviceStatus.ACTIVE, "10.0.0.2"));
        return UUID.fromString(response.path("data").path("id").asText());
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

    private Station station(Branch branch, String code, int stationNumber) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber(stationNumber);
        station.setStatus(StationStatus.AVAILABLE);
        return stationRepository.save(station);
    }

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("IoT User");
        user.setStatus(demo.server.common.enums.UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.save(user);
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
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private org.springframework.test.web.servlet.ResultActions patchJson(String path, String bearerToken, Object body) throws Exception {
        return mockMvc.perform(patch(path)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private JsonNode getJson(String path, String bearerToken) throws Exception {
        String response = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}

package demo.server.branch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.StationStatus;
import demo.server.dto.branch.BranchRequest;
import demo.server.dto.branch.StationRequest;
import demo.server.dto.branch.ZoneRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BranchStationManagementTests {

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
    void superAdminCreatesBranchZoneStationAndStationCodeIsUniqueInsideBranch() throws Exception {
        String token = token(admin("super@example.com", RoleCode.SUPER_ADMIN, null));
        JsonNode branch = postJson("/api/v1/admin/branches", token, branchRequest("HN01"));
        UUID branchId = UUID.fromString(branch.path("data").path("id").asText());

        JsonNode zone = postJson("/api/v1/admin/zones", token, new ZoneRequest(branchId, "VIP", "VIP Zone", "VIP", null, 1));
        UUID zoneId = UUID.fromString(zone.path("data").path("id").asText());

        StationRequest request = new StationRequest(branchId, zoneId, 1, "PC01", "PC 01", StationStatus.AVAILABLE, "10.0.0.1", "00:11");
        postJson("/api/v1/admin/stations", token, request);

        mockMvc.perform(post("/api/v1/admin/stations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StationRequest(branchId, zoneId, 2, "PC01", "PC 02", StationStatus.AVAILABLE, null, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void branchAdminIsScopedToOwnBranch() throws Exception {
        Branch own = branch("OWN");
        Branch other = branch("OTHER");
        String token = token(admin("branch@example.com", RoleCode.BRANCH_ADMIN, own));

        mockMvc.perform(post("/api/v1/admin/stations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StationRequest(other.getId(), null, 1, "PC01", "PC 01", StationStatus.AVAILABLE, null, null))))
                .andExpect(status().isForbidden());

        JsonNode list = getJson("/api/v1/admin/branches", token);
        assertThat(list.path("data").path("totalElements").asLong()).isEqualTo(1);
        assertThat(list.path("data").path("content").get(0).path("id").asText()).isEqualTo(own.getId().toString());
    }

    @Test
    void credentialSecretIsReturnedOnceAndRotationRevokesOldSecret() throws Exception {
        String token = token(admin("super-credential@example.com", RoleCode.SUPER_ADMIN, null));
        UUID branchId = UUID.fromString(postJson("/api/v1/admin/branches", token, branchRequest("SG01")).path("data").path("id").asText());
        UUID stationId = UUID.fromString(postJson("/api/v1/admin/stations", token,
                new StationRequest(branchId, null, 1, "PC01", "PC 01", StationStatus.OFFLINE, null, null))
                .path("data").path("id").asText());

        JsonNode created = postJson("/api/v1/admin/stations/" + stationId + "/credentials", token, "");
        String firstSecret = created.path("data").path("secret").asText();
        assertThat(firstSecret).isNotBlank();
        assertThat(credentialRepository.findAll().getFirst().getSecretHash()).doesNotContain(firstSecret);

        JsonNode rotated = postJson("/api/v1/admin/stations/" + stationId + "/credentials/rotate", token, "");
        String secondSecret = rotated.path("data").path("secret").asText();
        assertThat(secondSecret).isNotEqualTo(firstSecret);

        mockMvc.perform(post("/api/v1/stations/{id}/heartbeat", stationId)
                        .header("X-Station-Secret", firstSecret))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/stations/{id}/heartbeat", stationId)
                        .header("X-Station-Secret", secondSecret))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.lastSeenAt").exists());

        assertThat(stationRepository.findById(stationId).orElseThrow().getLastSeenAt()).isNotNull();
    }

    @Test
    void deleteStationIsSoftDelete() throws Exception {
        String token = token(admin("super-delete@example.com", RoleCode.SUPER_ADMIN, null));
        UUID branchId = UUID.fromString(postJson("/api/v1/admin/branches", token, branchRequest("DN01")).path("data").path("id").asText());
        UUID stationId = UUID.fromString(postJson("/api/v1/admin/stations", token,
                new StationRequest(branchId, null, 1, "PC01", "PC 01", StationStatus.AVAILABLE, null, null))
                .path("data").path("id").asText());

        mockMvc.perform(delete("/api/v1/admin/stations/{id}", stationId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        assertThat(stationRepository.findById(stationId).orElseThrow().isDeleted()).isTrue();
        assertThat(stationRepository.findById(stationId).orElseThrow().getStatus()).isEqualTo(StationStatus.DISABLED);
    }

    private BranchRequest branchRequest(String code) {
        return new BranchRequest(code, code + " Branch", "Address", "Asia/Ho_Chi_Minh",
                BranchStatus.ACTIVE, true, "PREPAID_OR_WALLET", LocalTime.of(8, 0), LocalTime.of(23, 0));
    }

    private Branch branch(String code) {
        Branch branch = new Branch();
        branch.setCode(code);
        branch.setName(code + " Branch");
        branch.setTimezone("Asia/Ho_Chi_Minh");
        branch.setPaymentPolicy("PREPAID_OR_WALLET");
        branch.setOperatingStartTime(LocalTime.of(8, 0));
        branch.setOperatingEndTime(LocalTime.of(23, 0));
        return branchRepository.save(branch);
    }

    private AppUser admin(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Admin");
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
        String json = body instanceof String text ? text : objectMapper.writeValueAsString(body);
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode getJson(String path, String bearerToken) throws Exception {
        String response = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}

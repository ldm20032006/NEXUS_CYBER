package demo.server.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.ModerationActionType;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserReportStatus;
import demo.server.common.enums.UserStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.social.UserReport;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.social.UserReportRepository;
import demo.server.service.social.SocialGuard;
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
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:nexus-social-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class SocialModerationTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired UserReportRepository reportRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SocialGuard socialGuard;

    Branch branchA;
    Branch branchB;

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
        branchA = branch("SOC-A");
        branchB = branch("SOC-B");
    }

    @Test
    void blockFiltersRadarAndInvitationThenUnblockRestoresVisibility() throws Exception {
        AppUser viewer = user("viewer@example.com", RoleCode.GAMER, branchA);
        AppUser blocked = user("blocked@example.com", RoleCode.GAMER, branchA);
        AppUser visible = user("visible@example.com", RoleCode.GAMER, branchA);
        String token = token(viewer);

        mockMvc.perform(post("/api/v1/social/blocks/{userId}", blocked.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"abuse"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blockedUserId").value(blocked.getId().toString()));

        String radar = mockMvc.perform(get("/api/v1/social/radar/users")
                        .header("Authorization", "Bearer " + token)
                        .param("branchId", branchA.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(radar).contains(visible.getId().toString()).doesNotContain(blocked.getId().toString());
        assertThat(socialGuard.canSendInvitation(viewer.getId(), blocked.getId())).isFalse();
        assertThat(socialGuard.canSendSocialNotification(viewer.getId(), blocked.getId())).isFalse();

        mockMvc.perform(delete("/api/v1/social/blocks/{userId}", blocked.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(socialGuard.canSendInvitation(viewer.getId(), blocked.getId())).isTrue();
    }

    @Test
    void cannotBlockSelfAndReportIsRateLimitedWithoutLeakingReporter() throws Exception {
        AppUser reporter = user("reporter@example.com", RoleCode.GAMER, branchA);
        AppUser target = user("target@example.com", RoleCode.GAMER, branchA);
        String token = token(reporter);

        mockMvc.perform(post("/api/v1/social/blocks/{userId}", reporter.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        String firstReport = report(token, target.getId(), "first report", 200);
        assertThat(firstReport).doesNotContain("reporterId", reporter.getId().toString());
        for (int i = 0; i < 4; i++) {
            report(token, target.getId(), "report " + i, 200);
        }
        report(token, target.getId(), "too many", 429);
    }

    @Test
    void adminModerationIsScopedByBranch() throws Exception {
        AppUser reporterA = user("reporter-a@example.com", RoleCode.GAMER, branchA);
        AppUser targetA = user("target-a@example.com", RoleCode.GAMER, branchA);
        AppUser reporterB = user("reporter-b@example.com", RoleCode.GAMER, branchB);
        AppUser targetB = user("target-b@example.com", RoleCode.GAMER, branchB);
        String reporterAToken = token(reporterA);
        String reporterBToken = token(reporterB);
        report(reporterAToken, targetA.getId(), "branch A", 200);
        report(reporterBToken, targetB.getId(), "branch B", 200);
        String adminToken = token(user("admin-a@example.com", RoleCode.BRANCH_ADMIN, branchA));

        String list = mockMvc.perform(get("/api/v1/admin/moderation/reports")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains(targetA.getId().toString()).doesNotContain(targetB.getId().toString());

        UserReport branchBReport = reportRepository.findByBranch_IdAndDeletedFalseOrderByReportedAtDesc(branchB.getId()).getFirst();
        mockMvc.perform(patch("/api/v1/admin/moderation/reports/{id}", branchBReport.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","action":"WARNING","note":"reviewed"}
                                """))
                .andExpect(status().isForbidden());

        UserReport branchAReport = reportRepository.findByBranch_IdAndDeletedFalseOrderByReportedAtDesc(branchA.getId()).getFirst();
        mockMvc.perform(patch("/api/v1/admin/moderation/reports/{id}", branchAReport.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED","action":"WARNING","note":"reviewed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(UserReportStatus.RESOLVED.name()))
                .andExpect(jsonPath("$.data.moderationAction").value(ModerationActionType.WARNING.name()));
    }

    private String report(String token, UUID targetId, String reason, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/v1/social/reports/{userId}", targetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"%s","context":"radar"}
                                """.formatted(reason)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    private String token(AppUser user) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"Password123"}
                                """.formatted(user.getEmail())))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.path("data").path("accessToken").asText();
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

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName(email);
        user.setDisplayName(email.substring(0, email.indexOf('@')));
        user.setStatus(UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }
}

package demo.server.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.NotificationType;
import demo.server.common.enums.RoleCode;
import demo.server.dto.notification.AccountSecurityNotificationRequest;
import demo.server.dto.notification.PushSubscriptionRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.notification.NotificationDeliveryRepository;
import demo.server.repository.notification.NotificationRepository;
import demo.server.repository.notification.PushSubscriptionRepository;
import demo.server.service.notification.NotificationService;
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
class NotificationDeliveryTests {

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
    NotificationRepository notificationRepository;

    @Autowired
    NotificationDeliveryRepository deliveryRepository;

    @Autowired
    PushSubscriptionRepository pushSubscriptionRepository;

    @Autowired
    NotificationService notificationService;

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
    void notificationIsVisibleOnlyToRecipientAndBranchScopedForAccountSecurity() throws Exception {
        Branch own = branch("NTF01");
        Branch other = branch("NTF02");
        AppUser ownAdmin = user("own-admin@example.com", RoleCode.BRANCH_ADMIN, own);
        AppUser gamer = user("gamer@example.com", RoleCode.GAMER, own);
        AppUser stranger = user("stranger@example.com", RoleCode.GAMER, other);
        String adminToken = token(ownAdmin);
        String gamerToken = token(gamer);
        String strangerToken = token(stranger);

        JsonNode created = postJson("/api/v1/admin/notifications/account-security", adminToken,
                new AccountSecurityNotificationRequest(gamer.getId(), "Login from gamer@example.com",
                        "Security event for gamer@example.com phone 0901234567"));
        UUID notificationId = UUID.fromString(created.path("data").path("id").asText());

        JsonNode ownList = getJson("/api/v1/notifications", gamerToken);
        assertThat(ownList.path("data").path("content")).hasSize(1);
        assertThat(ownList.path("data").path("content").get(0).path("content").asText()).doesNotContain("gamer@example.com");
        assertThat(ownList.path("data").path("content").get(0).path("content").asText()).doesNotContain("0901234567");

        JsonNode strangerList = getJson("/api/v1/notifications", strangerToken);
        assertThat(strangerList.path("data").path("content")).isEmpty();

        mockMvc.perform(get("/api/v1/notifications/{id}/deliveries", notificationId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/admin/notifications/account-security")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountSecurityNotificationRequest(stranger.getId(), "Cross", "Cross"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void markReadReadAllAndSoftHideDeleteWorkForRecipientOnly() throws Exception {
        Branch branch = branch("NTF03");
        AppUser gamer = user("read-gamer@example.com", RoleCode.GAMER, branch);
        AppUser other = user("other-gamer@example.com", RoleCode.GAMER, branch);
        notificationService.social(other.getId(), gamer.getId(), NotificationType.LOBBY_UPDATE, "Lobby", "Lobby update", "Lobby", UUID.randomUUID());
        notificationService.social(other.getId(), gamer.getId(), NotificationType.TEAM_INVITATION, "Invite", "Join", "Invitation", UUID.randomUUID());
        String token = token(gamer);
        UUID firstId = notificationRepository.findByUserIdOrderByCreatedAtDesc(gamer.getId()).getFirst().getId();

        mockMvc.perform(patch("/api/v1/notifications/{id}/read", firstId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.readAt").exists());

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(1));

        JsonNode count = getJson("/api/v1/notifications/unread-count", token);
        assertThat(count.path("data").path("count").asLong()).isZero();

        mockMvc.perform(delete("/api/v1/notifications/{id}", firstId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(notificationRepository.findById(firstId).orElseThrow().isDeleted()).isTrue();
        assertThat(notificationRepository.findById(firstId).orElseThrow().getHiddenAt()).isNotNull();
    }

    @Test
    void pushSubscribeCreatesDeliveryStatusAndUnsubscribeSoftDeletes() throws Exception {
        Branch branch = branch("NTF04");
        AppUser admin = user("push-admin@example.com", RoleCode.BRANCH_ADMIN, branch);
        AppUser gamer = user("push-gamer@example.com", RoleCode.GAMER, branch);
        String gamerToken = token(gamer);
        String adminToken = token(admin);

        JsonNode subscription = postJson("/api/v1/notifications/push-subscriptions", gamerToken,
                new PushSubscriptionRequest("https://push.example/subscription-1", "p256dh-key", "auth-key", "JUnit"));
        UUID subscriptionId = UUID.fromString(subscription.path("data").path("id").asText());
        assertThat(subscription.path("data").path("endpoint").asText()).contains("***");

        JsonNode notification = postJson("/api/v1/admin/notifications/account-security", adminToken,
                new AccountSecurityNotificationRequest(gamer.getId(), "Password changed", "Password changed for push-gamer@example.com"));
        UUID notificationId = UUID.fromString(notification.path("data").path("id").asText());
        JsonNode deliveries = getJson("/api/v1/notifications/" + notificationId + "/deliveries", gamerToken);
        assertThat(deliveries.path("data")).hasSize(3);
        assertThat(deliveryRepository.findByNotificationIdOrderByCreatedAtAsc(notificationId))
                .allMatch(delivery -> delivery.getAttemptCount() == 1);

        mockMvc.perform(delete("/api/v1/notifications/push-subscriptions/{id}", subscriptionId)
                        .header("Authorization", "Bearer " + gamerToken))
                .andExpect(status().isOk());
        assertThat(pushSubscriptionRepository.findById(subscriptionId).orElseThrow().isDeleted()).isTrue();
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

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Notification User");
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
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
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

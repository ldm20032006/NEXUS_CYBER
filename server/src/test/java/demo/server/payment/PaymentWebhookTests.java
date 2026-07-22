package demo.server.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.PaymentTransactionStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.TransactionType;
import demo.server.common.enums.UserStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.wallet.Wallet;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.payment.PaymentTransactionRepository;
import demo.server.repository.wallet.WalletRepository;
import demo.server.repository.wallet.WalletTransactionRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:nexus-payment-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "nexus.payment.webhook-secret=test-payment-webhook-secret"
})
class PaymentWebhookTests {

    private static final String SECRET = "test-payment-webhook-secret";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    WalletTransactionRepository walletTransactionRepository;

    @Autowired
    PaymentTransactionRepository paymentTransactionRepository;

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
    void validDuplicateCallbackCreditsWalletOnce() throws Exception {
        AppUser gamer = user("payment-success@example.com");
        String token = token(gamer);
        JsonNode topUp = topUp(token, "100.00", "topup-ok");
        String providerTransactionId = topUp.path("data").path("providerTransactionId").asText();
        String body = webhookBody(providerTransactionId, PaymentTransactionStatus.SUCCEEDED, "100.00");
        String timestamp = Instant.now().toString();
        String signature = signature(timestamp, body);

        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .header("X-Payment-Timestamp", timestamp)
                        .header("X-Payment-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(PaymentTransactionStatus.SUCCEEDED.name()))
                .andExpect(jsonPath("$.data.walletCredited").value(true));

        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .header("X-Payment-Timestamp", timestamp)
                        .header("X-Payment-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(PaymentTransactionStatus.SUCCEEDED.name()))
                .andExpect(jsonPath("$.data.walletCredited").value(false));

        Wallet wallet = walletRepository.findByUser_Id(gamer.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        assertThat(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()))
                .filteredOn(transaction -> transaction.getType() == TransactionType.TOP_UP)
                .hasSize(1);
        assertThat(paymentTransactionRepository.findByProviderAndProviderTransactionId("mock", providerTransactionId)
                .orElseThrow().getWalletTransaction()).isNotNull();
    }

    @Test
    void wrongSignatureIsRejectedAndDoesNotCreditWallet() throws Exception {
        AppUser gamer = user("payment-bad-signature@example.com");
        String token = token(gamer);
        JsonNode topUp = topUp(token, "50.00", "topup-bad-signature");
        String providerTransactionId = topUp.path("data").path("providerTransactionId").asText();
        String body = webhookBody(providerTransactionId, PaymentTransactionStatus.SUCCEEDED, "50.00");

        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .header("X-Payment-Timestamp", Instant.now().toString())
                        .header("X-Payment-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        assertThat(walletRepository.findByUser_Id(gamer.getId())).isEmpty();
    }

    @Test
    void oldTimestampReplayIsRejected() throws Exception {
        AppUser gamer = user("payment-replay@example.com");
        String token = token(gamer);
        JsonNode topUp = topUp(token, "75.00", "topup-replay");
        String providerTransactionId = topUp.path("data").path("providerTransactionId").asText();
        String body = webhookBody(providerTransactionId, PaymentTransactionStatus.SUCCEEDED, "75.00");
        String timestamp = Instant.now().minusSeconds(600).toString();

        mockMvc.perform(post("/api/v1/payments/webhooks/mock")
                        .header("X-Payment-Timestamp", timestamp)
                        .header("X-Payment-Signature", signature(timestamp, body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        assertThat(walletRepository.findByUser_Id(gamer.getId())).isEmpty();
    }

    private JsonNode topUp(String token, String amount, String idempotencyKey) throws Exception {
        String response = mockMvc.perform(post("/api/v1/payments/topups")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"%s"}
                                """.formatted(amount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.adapterMode").value("MOCK_DEVELOPMENT"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
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

    private String webhookBody(String providerTransactionId, PaymentTransactionStatus status, String amount) {
        return """
                {"providerTransactionId":"%s","status":"%s","amount":"%s","currency":"VND"}
                """.formatted(providerTransactionId, status.name(), amount).trim();
    }

    private String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    private AppUser user(String email) {
        Role role = roleRepository.findByCode(RoleCode.GAMER).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Gamer");
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }
}

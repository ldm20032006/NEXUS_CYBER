package demo.server.wallet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.TransactionType;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.TokenHashService;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.branch.StationCredential;
import demo.server.entity.session.PlaySession;
import demo.server.entity.session.SessionBillingPolicy;
import demo.server.entity.wallet.Wallet;
import demo.server.entity.wallet.WalletTransaction;
import demo.server.exception.BusinessRuleException;
import demo.server.dto.wallet.WalletTransactionResponse;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.session.SessionBillingPolicyRepository;
import demo.server.repository.wallet.WalletRepository;
import demo.server.repository.wallet.WalletTransactionRepository;
import demo.server.service.wallet.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:nexus-wallet-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class WalletBillingTests {

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
    SessionBillingPolicyRepository policyRepository;

    @Autowired
    PlaySessionRepository sessionRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    WalletTransactionRepository transactionRepository;

    @Autowired
    WalletService walletService;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    TokenHashService tokenHashService;

    Branch branch;
    Station station;
    String stationSecret = "wallet-station-secret";

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
        branch = branch("WAL01");
        station = station(branch, "PC01", StationStatus.AVAILABLE);
        credential(station, stationSecret);
        policy(branch, null, station, "60.00", "0.00", 15);
    }

    @Test
    void concurrentDebitsCannotMakeBalanceNegative() throws Exception {
        AppUser gamer = user("wallet-race@example.com", UserStatus.ACTIVE);
        walletService.adminAdjustment(gamer.getId(), new BigDecimal("100.00"), "seed balance", "seed-race");

        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first = () -> debit(gamer.getId(), "70.00", "race-debit-1");
            Callable<Boolean> second = () -> debit(gamer.getId(), "50.00", "race-debit-2");
            List<Boolean> results = executor.invokeAll(List.of(first, second)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception ex) {
                            throw new IllegalStateException(ex);
                        }
                    })
                    .toList();

            Wallet wallet = walletRepository.findByUser_Id(gamer.getId()).orElseThrow();
            assertThat(results).contains(true);
            assertThat(wallet.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()))
                    .filteredOn(transaction -> transaction.getAmount().compareTo(BigDecimal.ZERO) < 0)
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sessionBillingChargesWalletOnceAndRefundReferencesOriginalTransaction() throws Exception {
        AppUser gamer = user("wallet-billing@example.com", UserStatus.ACTIVE);
        walletService.adminAdjustment(gamer.getId(), new BigDecimal("100.00"), "seed balance", "seed-billing");
        String token = token(gamer);
        JsonNode qr = createQr("qr-wallet-billing");
        JsonNode active = confirm(UUID.fromString(qr.path("data").path("qrSessionId").asText()),
                qr.path("data").path("nonce").asText(), token, "confirm-wallet-billing");
        UUID sessionId = UUID.fromString(active.path("data").path("id").asText());
        PlaySession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setStartedAt(Instant.now().minusSeconds(30 * 60));
        sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/sessions/{id}/end", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"done"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(SessionStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.data.actualCost").value(30.00))
                .andExpect(jsonPath("$.data.endBalance").value(70.00));

        mockMvc.perform(post("/api/v1/sessions/{id}/end", sessionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        Wallet wallet = walletRepository.findByUser_Id(gamer.getId()).orElseThrow();
        List<WalletTransaction> transactions = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
        WalletTransaction charge = transactions.stream()
                .filter(transaction -> transaction.getType() == TransactionType.SESSION_CHARGE)
                .findFirst()
                .orElseThrow();
        assertThat(charge.getAmount()).isEqualByComparingTo("-30.00");
        assertThat(transactions).filteredOn(transaction -> transaction.getType() == TransactionType.SESSION_CHARGE).hasSize(1);

        var refund = walletService.refund(charge.getId(), "session refund", "refund-session");
        assertThat(refund.originalTransactionId()).isEqualTo(charge.getId());
        assertThat(walletRepository.findByUser_Id(gamer.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> walletService.refund(charge.getId(), "second refund", "refund-session-2"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void walletTransactionIsAppendOnly() {
        AppUser gamer = user("wallet-immutable@example.com", UserStatus.ACTIVE);
        WalletTransactionResponse response = walletService.adminAdjustment(gamer.getId(), new BigDecimal("10.00"), "seed balance", "seed-immutable");
        WalletTransaction transaction = transactionRepository.findById(response.id()).orElseThrow();
        transaction.setDescription("changed");

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(transaction))
                .isInstanceOfAny(UnsupportedOperationException.class, ObjectOptimisticLockingFailureException.class);
    }

    private boolean debit(UUID userId, String amount, String key) {
        try {
            walletService.adminAdjustment(userId, new BigDecimal(amount).negate(), "debit", key);
            return true;
        } catch (BusinessRuleException | demo.server.exception.ConcurrencyConflictException ex) {
            return false;
        }
    }

    private JsonNode createQr(String idempotencyKey) throws Exception {
        String response = mockMvc.perform(post("/api/v1/qr-sessions")
                        .header("X-Station-Id", station.getId().toString())
                        .header("X-Station-Secret", stationSecret)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode confirm(UUID qrId, String nonce, String token, String idempotencyKey) throws Exception {
        String response = mockMvc.perform(post("/api/v1/qr-sessions/{id}/confirm", qrId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nonce":"%s"}
                                """.formatted(nonce)))
                .andExpect(status().isOk())
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

    private Station station(Branch branch, String code, StationStatus status) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber(1);
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

    private SessionBillingPolicy policy(Branch branch, demo.server.entity.branch.Zone zone, Station station,
                                        String hourlyRate, String minimumCharge, int incrementMinutes) {
        SessionBillingPolicy policy = new SessionBillingPolicy();
        policy.setBranch(branch);
        policy.setZone(zone);
        policy.setStation(station);
        policy.setHourlyRate(new BigDecimal(hourlyRate));
        policy.setMinimumCharge(new BigDecimal(minimumCharge));
        policy.setBillingIncrementMinutes(incrementMinutes);
        policy.setActive(true);
        return policyRepository.save(policy);
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

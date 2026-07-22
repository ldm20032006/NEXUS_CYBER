package demo.server.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.DeviceStatus;
import demo.server.common.enums.DeviceType;
import demo.server.common.enums.OrderStatus;
import demo.server.common.enums.PaymentMethod;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.TransactionType;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.iot.DeviceAlert;
import demo.server.entity.iot.IotDevice;
import demo.server.entity.ordering.FoodOrder;
import demo.server.entity.session.PlaySession;
import demo.server.entity.wallet.Wallet;
import demo.server.entity.wallet.WalletTransaction;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.iot.DeviceAlertRepository;
import demo.server.repository.iot.IotDeviceRepository;
import demo.server.repository.ordering.FoodOrderRepository;
import demo.server.repository.session.PlaySessionRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReportServiceTests {

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
    PlaySessionRepository sessionRepository;

    @Autowired
    FoodOrderRepository orderRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    WalletTransactionRepository transactionRepository;

    @Autowired
    IotDeviceRepository deviceRepository;

    @Autowired
    DeviceAlertRepository alertRepository;

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
    void emptyOverviewReturnsTimezoneRangeAndZeroMetrics() throws Exception {
        Branch branch = branch("RPT01");
        String token = token(user("rpt-admin@example.com", RoleCode.BRANCH_ADMIN, branch));

        JsonNode response = getJson("/api/v1/admin/dashboard/overview?period=date&timezone=Asia/Ho_Chi_Minh", token);

        assertThat(response.path("data").path("timezone").asText()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(response.path("data").path("from").asText()).isNotBlank();
        assertThat(response.path("data").path("to").asText()).isNotBlank();
        assertThat(response.path("data").path("generatedAt").asText()).isNotBlank();
        assertThat(response.path("data").path("revenue").path("netRevenue").decimalValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void revenueSubtractsRefundAndKeepsDebitRevenuePositive() throws Exception {
        Branch branch = branch("RPT02");
        Station station = station(branch, "PC01", StationStatus.OCCUPIED);
        AppUser gamer = user("rpt-gamer@example.com", RoleCode.GAMER, branch);
        AppUser admin = user("rpt-admin2@example.com", RoleCode.BRANCH_ADMIN, branch);
        Wallet wallet = wallet(gamer, "1000.00");
        PlaySession session = session(gamer, station);
        FoodOrder order = order(gamer, branch, station, session, "200.00");
        tx(wallet, gamer, TransactionType.SESSION_CHARGE, "-120.00", "PLAY_SESSION", session.getId().toString(), null);
        WalletTransaction orderPayment = tx(wallet, gamer, TransactionType.ORDER_PAYMENT, "-200.00", "FOOD_ORDER", order.getId().toString(), null);
        tx(wallet, gamer, TransactionType.TOP_UP, "500.00", "PAYMENT_TRANSACTION", UUID.randomUUID().toString(), null);
        tx(wallet, gamer, TransactionType.REFUND, "50.00", "FOOD_ORDER", order.getId().toString(), orderPayment);

        JsonNode response = getJson("/api/v1/admin/reports/revenue?period=date&timezone=UTC", token(admin));

        assertThat(response.path("data").path("sessionRevenue").decimalValue()).isEqualByComparingTo("120.00");
        assertThat(response.path("data").path("foodRevenue").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(response.path("data").path("topUpRevenue").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(response.path("data").path("refundAmount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(response.path("data").path("netRevenue").decimalValue()).isEqualByComparingTo("770.00");
    }

    @Test
    void branchScopePreventsCrossBranchReport() throws Exception {
        Branch own = branch("RPT03");
        Branch other = branch("RPT04");
        String token = token(user("scope-admin@example.com", RoleCode.BRANCH_ADMIN, own));

        mockMvc.perform(get("/api/v1/admin/dashboard/overview")
                        .queryParam("branchId", other.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void csvExportUsesFilterAndIncludesFormulas() throws Exception {
        Branch branch = branch("RPT05");
        Station station = station(branch, "PC01", StationStatus.OCCUPIED);
        AppUser admin = user("csv-admin@example.com", RoleCode.BRANCH_ADMIN, branch);
        AppUser gamer = user("csv-gamer@example.com", RoleCode.GAMER, branch);
        session(gamer, station);
        IotDevice device = device(branch, station);
        DeviceAlert alert = new DeviceAlert();
        alert.setBranch(branch);
        alert.setStation(station);
        alert.setDevice(device);
        alert.setAlertCode("HEARTBEAT_MISSED");
        alert.setTitle("Offline");
        alert.setDescription("Device offline");
        alertRepository.save(alert);

        String csv = mockMvc.perform(get("/api/v1/admin/reports/export")
                        .queryParam("type", "overview")
                        .queryParam("format", "csv")
                        .queryParam("timezone", "Asia/Ho_Chi_Minh")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andReturn().getResponse().getContentAsString();

        assertThat(csv).contains("from,to,timezone,generatedAt,metric,value,unit,formula");
        assertThat(csv).contains("Asia/Ho_Chi_Minh");
        assertThat(csv).contains("device.failures");
        assertThat(csv).contains("netRevenue = SESSION_CHARGE + ORDER_PAYMENT + TOP_UP - REFUND");
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

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Report User");
        user.setStatus(demo.server.common.enums.UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private Wallet wallet(AppUser user, String balance) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal(balance));
        return walletRepository.save(wallet);
    }

    private PlaySession session(AppUser user, Station station) {
        PlaySession session = new PlaySession();
        session.setUser(user);
        session.setStation(station);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        return sessionRepository.save(session);
    }

    private FoodOrder order(AppUser user, Branch branch, Station station, PlaySession session, String amount) {
        FoodOrder order = new FoodOrder();
        order.setUser(user);
        order.setBranch(branch);
        order.setStation(station);
        order.setPlaySession(session);
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentMethod(PaymentMethod.WALLET);
        order.setTotalAmount(new BigDecimal(amount));
        return orderRepository.save(order);
    }

    private WalletTransaction tx(Wallet wallet, AppUser user, TransactionType type, String amount,
                                 String referenceType, String referenceId, WalletTransaction original) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setBalanceBefore(BigDecimal.ZERO);
        transaction.setBalanceAfter(BigDecimal.ZERO);
        transaction.setReferenceType(referenceType);
        transaction.setReferenceId(referenceId);
        transaction.setOriginalTransaction(original);
        return transactionRepository.save(transaction);
    }

    private IotDevice device(Branch branch, Station station) {
        IotDevice device = new IotDevice();
        device.setBranch(branch);
        device.setStation(station);
        device.setDeviceType(DeviceType.IOT_GATEWAY);
        device.setSerialNumber("RPT-DEVICE-" + UUID.randomUUID());
        device.setStatus(DeviceStatus.OFFLINE);
        return deviceRepository.save(device);
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

    private JsonNode getJson(String path, String bearerToken) throws Exception {
        String response = mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + bearerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}

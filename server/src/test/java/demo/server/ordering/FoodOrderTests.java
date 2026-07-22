package demo.server.ordering;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.BranchStatus;
import demo.server.common.enums.MenuItemStatus;
import demo.server.common.enums.OrderStatus;
import demo.server.common.enums.PaymentMethod;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.SessionStatus;
import demo.server.common.enums.StationStatus;
import demo.server.common.enums.UserStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Station;
import demo.server.entity.ordering.MenuCategory;
import demo.server.entity.ordering.MenuItem;
import demo.server.entity.session.PlaySession;
import demo.server.entity.wallet.Wallet;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.RoleRepository;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.branch.StationRepository;
import demo.server.repository.ordering.MenuCategoryRepository;
import demo.server.repository.ordering.MenuItemRepository;
import demo.server.repository.ordering.OrderItemRepository;
import demo.server.repository.session.PlaySessionRepository;
import demo.server.repository.wallet.WalletRepository;
import demo.server.repository.wallet.WalletTransactionRepository;
import demo.server.service.wallet.WalletService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:nexus-order-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class FoodOrderTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired AppUserRepository appUserRepository;
    @Autowired BranchRepository branchRepository;
    @Autowired StationRepository stationRepository;
    @Autowired PlaySessionRepository sessionRepository;
    @Autowired MenuCategoryRepository categoryRepository;
    @Autowired MenuItemRepository menuItemRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository walletTransactionRepository;
    @Autowired WalletService walletService;
    @Autowired PasswordEncoder passwordEncoder;

    Branch branch;
    Station station;
    MenuCategory category;
    MenuItem item;

    @BeforeEach
    void setUp() {
        Arrays.stream(RoleCode.values()).forEach(code -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(code.name());
            roleRepository.save(role);
        });
        branch = branch("ORD01", "PREPAID_OR_WALLET");
        station = station(branch, "PC01");
        category = category(branch, "DRINK");
        item = item(branch, category, "COFFEE", "Coffee", "25.00", 2);
    }

    @Test
    void createsWalletOrderWithServerPriceSnapshotAndStockDebit() throws Exception {
        AppUser gamer = user("order-money@example.com", RoleCode.GAMER, null);
        activeSession(gamer, station);
        walletService.adminAdjustment(gamer.getId(), new BigDecimal("100.00"), "seed", "seed-order-money");
        String token = token(gamer);

        JsonNode order = createOrder(token, item.getId(), 2, PaymentMethod.WALLET, "order-money", 200);

        assertThat(order.path("data").path("totalAmount").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(order.path("data").path("items").get(0).path("unitPrice").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(order.path("data").path("items").get(0).path("subtotal").decimalValue()).isEqualByComparingTo("50.00");
        assertThat(menuItemRepository.findById(item.getId()).orElseThrow().getStockQuantity()).isZero();
        Wallet wallet = walletRepository.findByUser_Id(gamer.getId()).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("50.00");

        MenuItem changed = menuItemRepository.findById(item.getId()).orElseThrow();
        changed.setPrice(new BigDecimal("99.00"));
        menuItemRepository.save(changed);
        UUID orderId = UUID.fromString(order.path("data").path("id").asText());
        assertThat(orderItemRepository.findByOrderId(orderId).getFirst().getUnitPrice()).isEqualByComparingTo("25.00");
    }

    @Test
    void cancelNewWalletOrderRefundsAndRestoresStock() throws Exception {
        AppUser gamer = user("order-cancel@example.com", RoleCode.GAMER, null);
        activeSession(gamer, station);
        walletService.adminAdjustment(gamer.getId(), new BigDecimal("100.00"), "seed", "seed-order-cancel");
        String token = token(gamer);
        JsonNode order = createOrder(token, item.getId(), 1, PaymentMethod.WALLET, "order-cancel", 200);
        UUID orderId = UUID.fromString(order.path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""" 
                                {"reason":"changed mind"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(OrderStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.data.cancelReason").value("changed mind"));

        assertThat(menuItemRepository.findById(item.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(walletRepository.findByUser_Id(gamer.getId()).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(walletRepository.findByUser_Id(gamer.getId()).orElseThrow().getId()))
                .hasSize(3);
    }

    @Test
    void staffBranchStateMachineAllowsOnlyValidTransitions() throws Exception {
        AppUser gamer = user("order-state@example.com", RoleCode.GAMER, null);
        activeSession(gamer, station);
        String gamerToken = token(gamer);
        JsonNode order = createOrder(gamerToken, item.getId(), 1, PaymentMethod.PAY_AT_COUNTER, "order-state", 200);
        UUID orderId = UUID.fromString(order.path("data").path("id").asText());
        String staffToken = token(user("staff-fnb@example.com", RoleCode.STAFF_FNB, branch));

        updateStatus(staffToken, orderId, OrderStatus.PREPARING, 422);
        updateStatus(staffToken, orderId, OrderStatus.ACCEPTED, 200);
        updateStatus(staffToken, orderId, OrderStatus.PREPARING, 200);
        updateStatus(staffToken, orderId, OrderStatus.READY, 200);
        updateStatus(staffToken, orderId, OrderStatus.DELIVERED, 200);

        mockMvc.perform(get("/api/v1/staff/orders")
                        .header("Authorization", "Bearer " + staffToken)
                        .param("status", OrderStatus.DELIVERED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value(OrderStatus.DELIVERED.name()));
    }

    @Test
    void concurrentOrdersDoNotOversellStock() throws Exception {
        MenuItem limited = item(branch, category, "WATER", "Water", "10.00", 1);
        AppUser gamer = user("order-race@example.com", RoleCode.GAMER, null);
        activeSession(gamer, station);
        walletService.adminAdjustment(gamer.getId(), new BigDecimal("100.00"), "seed", "seed-order-race");
        String token = token(gamer);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> first = () -> createOrderStatus(token, limited.getId(), "order-race-1");
            Callable<Integer> second = () -> createOrderStatus(token, limited.getId(), "order-race-2");
            List<Integer> statuses = executor.invokeAll(List.of(first, second)).stream().map(future -> {
                try {
                    return future.get();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }).toList();
            assertThat(statuses).contains(200);
            assertThat(menuItemRepository.findById(limited.getId()).orElseThrow().getStockQuantity()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    private JsonNode createOrder(String token, UUID itemId, int quantity, PaymentMethod paymentMethod,
                                 String idempotencyKey, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"%s","items":[{"menuItemId":"%s","quantity":%d}]}
                                """.formatted(paymentMethod.name(), itemId, quantity)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private int createOrderStatus(String token, UUID itemId, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"WALLET","items":[{"menuItemId":"%s","quantity":1}]}
                                """.formatted(itemId)))
                .andReturn().getResponse().getStatus();
    }

    private void updateStatus(String token, UUID orderId, OrderStatus status, int expectedStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/staff/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}
                                """.formatted(status.name())))
                .andExpect(status().is(expectedStatus));
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

    private Station station(Branch branch, String code) {
        Station station = new Station();
        station.setBranch(branch);
        station.setCode(code);
        station.setName(code);
        station.setStationNumber(1);
        station.setStatus(StationStatus.OCCUPIED);
        return stationRepository.save(station);
    }

    private MenuCategory category(Branch branch, String code) {
        MenuCategory category = new MenuCategory();
        category.setBranch(branch);
        category.setCode(code);
        category.setName(code);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private MenuItem item(Branch branch, MenuCategory category, String code, String name, String price, int stock) {
        MenuItem item = new MenuItem();
        item.setBranch(branch);
        item.setCategory(category);
        item.setCode(code);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setStockQuantity(stock);
        item.setStatus(MenuItemStatus.ACTIVE);
        return menuItemRepository.save(item);
    }

    private PlaySession activeSession(AppUser user, Station station) {
        PlaySession session = new PlaySession();
        session.setUser(user);
        session.setStation(station);
        session.setStatus(SessionStatus.ACTIVE);
        session.setStartedAt(Instant.now());
        return sessionRepository.save(session);
    }

    private AppUser user(String email, RoleCode roleCode, Branch branch) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setBranch(branch);
        user.getRoles().add(role);
        return appUserRepository.save(user);
    }
}

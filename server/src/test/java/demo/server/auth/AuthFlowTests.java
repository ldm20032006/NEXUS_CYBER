package demo.server.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.common.security.TokenHashService;
import demo.server.dto.auth.RegisterGamerRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.PasswordResetToken;
import demo.server.entity.auth.Role;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.auth.PasswordResetTokenRepository;
import demo.server.repository.auth.RoleRepository;
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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthFlowTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    TokenHashService tokenHashService;

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
    void registerLoginByPhoneAndCurrentUser() throws Exception {
        JsonNode register = postJson("/api/v1/auth/register", new RegisterGamerRequest(
                "gamer@example.com", "0900000001", "Password123", "Gamer One", "Gamer"));

        assertThat(register.path("data").path("accessToken").asText()).isNotBlank();
        assertThat(register.path("data").path("user").path("roles").get(0).asText()).isEqualTo("GAMER");

        JsonNode login = postJson("/api/v1/auth/login", """
                {"identifier":"0900000001","password":"Password123"}
                """);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + login.path("data").path("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("gamer@example.com"));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        postJson("/api/v1/auth/register", new RegisterGamerRequest(
                "duplicate@example.com", null, "Password123", "Gamer One", null));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterGamerRequest(
                                "duplicate@example.com", null, "Password123", "Gamer Two", null))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterGamerRequest(
                                "duplicate@example.com", null, "Password123", "Gamer Two", null))))
                .andExpect(jsonPath("$.message").value("Account already exists"));
    }

    @Test
    void lockedUserCannotLogin() throws Exception {
        AppUser user = gamer("locked@example.com", "0900000002", UserStatus.LOCKED);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"locked@example.com","password":"Password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is locked"));

        assertThat(user.getId()).isNotNull();
    }

    @Test
    void refreshRotationDetectsReuseAndRevokesFamily() throws Exception {
        JsonNode login = postJson("/api/v1/auth/register", new RegisterGamerRequest(
                "refresh@example.com", null, "Password123", "Refresh User", null));
        String originalRefreshToken = login.path("data").path("refreshToken").asText();

        JsonNode rotated = postJson("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(originalRefreshToken));
        assertThat(rotated.path("data").path("refreshToken").asText()).isNotEqualTo(originalRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(originalRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token reuse detected"));
    }

    @Test
    void resetAndChangePasswordUseSingleUseTokens() throws Exception {
        AppUser user = gamer("reset@example.com", null, UserStatus.ACTIVE);
        String rawResetToken = "raw-reset-token";
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(tokenHashService.hash(rawResetToken));
        token.setIssuedAt(Instant.now());
        token.setExpiresAt(Instant.now().plusSeconds(1800));
        passwordResetTokenRepository.save(token);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"raw-reset-token","newPassword":"NewPassword123"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"raw-reset-token","newPassword":"OtherPassword123"}
                                """))
                .andExpect(status().isUnauthorized());

        JsonNode login = postJson("/api/v1/auth/login", """
                {"identifier":"reset@example.com","password":"NewPassword123"}
                """);
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + login.path("data").path("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"NewPassword123","newPassword":"ChangedPassword123"}
                                """))
                .andExpect(status().isOk());
    }

    private AppUser gamer(String email, String phone, UserStatus status) {
        Role gamer = roleRepository.findByCode(RoleCode.GAMER).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("Test Gamer");
        user.setStatus(status);
        user.getRoles().add(gamer);
        return appUserRepository.save(user);
    }

    private JsonNode postJson(String path, Object body) throws Exception {
        String json = body instanceof String stringBody ? stringBody : objectMapper.writeValueAsString(body);
        String response = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }
}

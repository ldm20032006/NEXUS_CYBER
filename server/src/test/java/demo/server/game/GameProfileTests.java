package demo.server.game;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.RoleCode;
import demo.server.common.enums.UserStatus;
import demo.server.dto.game.GameRankRequest;
import demo.server.dto.game.GameRequest;
import demo.server.dto.game.GameRoleRequest;
import demo.server.dto.gamer.GamerGameProfileRequest;
import demo.server.dto.gamer.GamerProfileRequest;
import demo.server.dto.gamer.StationPreferenceRequest;
import demo.server.entity.auth.AppUser;
import demo.server.entity.auth.Role;
import demo.server.repository.auth.AppUserRepository;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameProfileTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    AppUserRepository appUserRepository;

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
    void adminCreatesGameAndUniqueSlugIsEnforced() throws Exception {
        String adminToken = token(user("admin-game@example.com", RoleCode.SUPER_ADMIN));
        postJson("/api/v1/admin/games", adminToken, game("valorant"));

        mockMvc.perform(post("/api/v1/admin/games")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(game("valorant"))))
                .andExpect(status().isConflict());
    }

    @Test
    void gameProfileRequiresRankAndRoleBelongToSelectedGameAndIsUniquePerUserGame() throws Exception {
        String adminToken = token(user("admin-profile-game@example.com", RoleCode.SUPER_ADMIN));
        UUID gameA = UUID.fromString(postJson("/api/v1/admin/games", adminToken, game("lol")).path("data").path("id").asText());
        UUID gameB = UUID.fromString(postJson("/api/v1/admin/games", adminToken, game("dota")).path("data").path("id").asText());
        UUID rankA = UUID.fromString(postJson("/api/v1/admin/games/" + gameA + "/ranks", adminToken, new GameRankRequest("GOLD", "Gold", 1)).path("data").path("id").asText());
        UUID roleA = UUID.fromString(postJson("/api/v1/admin/games/" + gameA + "/roles", adminToken, new GameRoleRequest("MID", "Mid", 1)).path("data").path("id").asText());
        UUID rankB = UUID.fromString(postJson("/api/v1/admin/games/" + gameB + "/ranks", adminToken, new GameRankRequest("HERALD", "Herald", 1)).path("data").path("id").asText());

        String gamerToken = token(user("gamer-game-profile@example.com", RoleCode.GAMER));
        postJson("/api/v1/profiles/me/game-profiles", gamerToken,
                new GamerGameProfileRequest(gameA, "PlayerOne", rankA, roleA, null, "Aggressive", "Duelist", true));

        mockMvc.perform(post("/api/v1/profiles/me/game-profiles")
                        .header("Authorization", "Bearer " + gamerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GamerGameProfileRequest(gameA, "PlayerTwo", rankA, roleA, null, null, null, true))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/profiles/me/game-profiles")
                        .header("Authorization", "Bearer " + gamerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GamerGameProfileRequest(gameB, "BadRank", rankA, null, null, null, null, true))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Rank must belong to selected game"));

        assertThat(rankB).isNotNull();
    }

    @Test
    void gameProfileOwnershipIsEnforced() throws Exception {
        String adminToken = token(user("admin-ownership@example.com", RoleCode.SUPER_ADMIN));
        UUID gameId = UUID.fromString(postJson("/api/v1/admin/games", adminToken, game("cs2")).path("data").path("id").asText());
        String ownerToken = token(user("owner@example.com", RoleCode.GAMER));
        String otherToken = token(user("other@example.com", RoleCode.GAMER));
        UUID profileId = UUID.fromString(postJson("/api/v1/profiles/me/game-profiles", ownerToken,
                new GamerGameProfileRequest(gameId, "Owner", null, null, null, null, null, true)).path("data").path("id").asText());

        mockMvc.perform(delete("/api/v1/profiles/me/game-profiles/{id}", profileId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicGamerProfileDoesNotExposeEmailPhoneOrBalanceAndAvatarIsUrlOnly() throws Exception {
        String gamerToken = token(user("private@example.com", RoleCode.GAMER));

        JsonNode profile = putJson("/api/v1/profiles/me", gamerToken,
                new GamerProfileRequest("Nick", "https://cdn.example.com/avatar.png", LocalDate.of(2000, 1, 1), 170, 70, true, "Bio"));

        String json = profile.path("data").toString();
        assertThat(json).contains("avatar.png");
        assertThat(json).doesNotContain("private@example.com", "phone", "balance");
    }

    @Test
    void stationPreferenceValidationRejectsOutOfRangeAndAcceptsValidValues() throws Exception {
        String gamerToken = token(user("preference@example.com", RoleCode.GAMER));

        mockMvc.perform(put("/api/v1/profiles/me/station-preference")
                        .header("Authorization", "Bearer " + gamerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deskHeightCm":59,"chairAngleDegree":146,"rgbColor":"blue","brightness":101,"mouseDpi":100}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/v1/profiles/me/station-preference")
                        .header("Authorization", "Bearer " + gamerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deskHeightCm":100,"chairAngleDegree":120,"rgbColor":"#A1B2C3","brightness":80,"mouseDpi":1600,"nightMode":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rgbColor").value("#A1B2C3"));
    }

    private GameRequest game(String slug) {
        return new GameRequest(slug, slug.toUpperCase(), "Game", 5, null);
    }

    private AppUser user(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Password123"));
        user.setFullName("User");
        user.setStatus(UserStatus.ACTIVE);
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

    private JsonNode putJson(String path, String bearerToken, Object body) throws Exception {
        String response = mockMvc.perform(put(path)
                        .header("Authorization", "Bearer " + bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}

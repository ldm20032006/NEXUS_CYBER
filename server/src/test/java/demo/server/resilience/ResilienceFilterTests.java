package demo.server.resilience;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResilienceFilterTests {

    @Autowired
    MockMvc mockMvc;

    @Test
    void loginRateLimitReturnsTooManyRequests() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"identifier":"missing@example.com","password":"Password123"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"missing@example.com","password":"Password123"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }

    @Test
    void qrConfirmAndTeamInvitationAreRateLimitedOnTheirActualPaths() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/qr-sessions/" + java.util.UUID.randomUUID() + "/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nonce\":\"abc\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        mockMvc.perform(post("/api/v1/qr-sessions/" + java.util.UUID.randomUUID() + "/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nonce\":\"abc\"}"))
                .andExpect(status().isTooManyRequests());

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/team-invitations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"receiverId\":\"00000000-0000-0000-0000-000000000001\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        mockMvc.perform(post("/api/v1/team-invitations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiverId\":\"00000000-0000-0000-0000-000000000001\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void idempotencyKeyTracksRequestStatusAndRejectsReplay() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "order-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"coffee\",\"quantity\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Idempotency-Status", "STARTED"));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "order-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"coffee\",\"quantity\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Idempotency Replay"));
    }

    @Test
    void idempotencyKeyRejectsFingerprintMismatch() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "order-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"coffee\",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", "order-key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"coffee\",\"quantity\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Idempotency Conflict"));
    }
}

package demo.server.common.resilience;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.logging.CorrelationIdFilter;
import demo.server.common.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "Idempotency-Key";

    private final IdempotencyService idempotencyService;
    private final ResilienceKeys keys;
    private final ResilienceProperties properties;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(
            IdempotencyService idempotencyService,
            ResilienceKeys keys,
            ResilienceProperties properties,
            ObjectMapper objectMapper
    ) {
        this.idempotencyService = idempotencyService;
        this.keys = keys;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = request.getHeader(HEADER_NAME);
        if (!requiresIdempotency(request) || !StringUtils.hasText(idempotencyKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, body);
        String key = keys.idempotency(actionFor(request), idempotencyKey.trim());
        String fingerprint = fingerprint(request, body);
        try {
            IdempotencyDecision decision = idempotencyService.begin(key, fingerprint, properties.idempotencyTtl());
            response.setHeader("Idempotency-Status", decision.record().status().name());
            if (decision.type() == IdempotencyDecisionType.FINGERPRINT_MISMATCH) {
                writeError(response, HttpStatus.CONFLICT, "Idempotency Conflict", "Idempotency-Key was reused with a different request", request.getRequestURI());
                return;
            }
            if (decision.type() == IdempotencyDecisionType.IN_PROGRESS) {
                writeError(response, HttpStatus.CONFLICT, "Idempotency In Progress", "Request with this Idempotency-Key is still processing", request.getRequestURI());
                return;
            }
            if (decision.type() == IdempotencyDecisionType.REPLAY) {
                writeError(response, HttpStatus.CONFLICT, "Idempotency Replay", "Request with this Idempotency-Key was already processed", request.getRequestURI());
                return;
            }
            filterChain.doFilter(cachedRequest, response);
            if (response.getStatus() >= 500) {
                idempotencyService.fail(key, response.getStatus());
            } else {
                idempotencyService.complete(key, response.getStatus());
            }
        } catch (RuntimeException ex) {
            if (properties.idempotencyFailurePolicy() == ResilienceProperties.FailurePolicy.FAIL_CLOSED) {
                writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "Idempotency service is unavailable", request.getRequestURI());
                return;
            }
            filterChain.doFilter(cachedRequest, response);
        }
    }

    private boolean requiresIdempotency(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)) {
            return false;
        }
        String path = request.getRequestURI();
        return (path.startsWith("/api/v1/qr/") && path.endsWith("/confirm"))
                || path.startsWith("/api/v1/sessions")
                || path.startsWith("/api/v1/wallet")
                || path.startsWith("/api/v1/payment-callbacks")
                || path.startsWith("/api/v1/stock")
                || path.endsWith("/accept") && path.startsWith("/api/v1/invitations")
                || path.startsWith("/api/v1/orders");
    }

    private String actionFor(HttpServletRequest request) {
        return request.getMethod().toLowerCase() + ":" + request.getRequestURI();
    }

    private String fingerprint(HttpServletRequest request, byte[] body) {
        String query = request.getQueryString() == null ? "" : request.getQueryString();
        return sha256(request.getMethod() + "\n" + request.getRequestURI() + "\n" + query + "\n" + sha256(body));
    }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(value));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String error, String message, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .correlationId(MDC.get(CorrelationIdFilter.MDC_KEY))
                .build());
    }
}

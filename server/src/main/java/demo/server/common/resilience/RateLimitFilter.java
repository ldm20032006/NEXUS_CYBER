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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ResilienceKeys keys;
    private final ResilienceProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            ResilienceKeys keys,
            ResilienceProperties properties,
            ObjectMapper objectMapper
    ) {
        this.rateLimitService = rateLimitService;
        this.keys = keys;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitRule rule = ruleFor(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            RateLimitDecision decision = rateLimitService.consume(
                    keys.rateLimit(rule.action(), clientIp(request)),
                    rule.limit().permits(),
                    rule.limit().window());
            response.setHeader("X-RateLimit-Remaining", Integer.toString(decision.remaining()));
            response.setHeader("X-RateLimit-Reset", decision.resetAt().toString());
            if (!decision.allowed()) {
                writeError(response, HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", "Rate limit exceeded", request.getRequestURI());
                return;
            }
        } catch (RuntimeException ex) {
            if (properties.rateLimitFailurePolicy() == ResilienceProperties.FailurePolicy.FAIL_CLOSED) {
                writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", "Rate limit service is unavailable", request.getRequestURI());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private RateLimitRule ruleFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }
        String path = request.getRequestURI();
        if ("/api/v1/auth/login".equals(path)) {
            return new RateLimitRule("auth-login", properties.login());
        }
        if ("/api/v1/auth/forgot-password".equals(path)) {
            return new RateLimitRule("auth-forgot-password", properties.forgotPassword());
        }
        if ("/api/v1/auth/reset-password".equals(path)) {
            return new RateLimitRule("auth-reset-password", properties.resetPassword());
        }
        if ((path.startsWith("/api/v1/qr/") || path.startsWith("/api/v1/qr-sessions/")) && path.endsWith("/confirm")) {
            return new RateLimitRule("qr-confirm", properties.qrConfirm());
        }
        if (path.startsWith("/api/v1/invitations") || path.startsWith("/api/v1/team-invitations")) {
            return new RateLimitRule("invitation", properties.invitation());
        }
        if (path.startsWith("/api/v1/orders")) {
            return new RateLimitRule("order", properties.order());
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null ? request.getRemoteAddr() : forwardedFor.split(",")[0].trim();
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

    private record RateLimitRule(String action, ResilienceProperties.RateLimit limit) {
    }
}

package demo.server.common.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus.resilience")
public record ResilienceProperties(
        String keyPrefix,
        FailurePolicy cacheFailurePolicy,
        FailurePolicy rateLimitFailurePolicy,
        FailurePolicy idempotencyFailurePolicy,
        FailurePolicy lockFailurePolicy,
        Duration defaultTtl,
        Duration lockTimeout,
        Duration idempotencyTtl,
        Duration onlineStateTtl,
        RateLimit login,
        RateLimit forgotPassword,
        RateLimit resetPassword,
        RateLimit qrConfirm,
        RateLimit invitation,
        RateLimit order
) {
    public ResilienceProperties {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            keyPrefix = "nexus";
        }
        if (cacheFailurePolicy == null) {
            cacheFailurePolicy = FailurePolicy.FAIL_OPEN;
        }
        if (rateLimitFailurePolicy == null) {
            rateLimitFailurePolicy = FailurePolicy.FAIL_CLOSED;
        }
        if (idempotencyFailurePolicy == null) {
            idempotencyFailurePolicy = FailurePolicy.FAIL_CLOSED;
        }
        if (lockFailurePolicy == null) {
            lockFailurePolicy = FailurePolicy.FAIL_CLOSED;
        }
        if (defaultTtl == null) {
            defaultTtl = Duration.ofMinutes(5);
        }
        if (lockTimeout == null) {
            lockTimeout = Duration.ofSeconds(10);
        }
        if (idempotencyTtl == null) {
            idempotencyTtl = Duration.ofHours(24);
        }
        if (onlineStateTtl == null) {
            onlineStateTtl = Duration.ofMinutes(2);
        }
        if (login == null) {
            login = new RateLimit(5, Duration.ofMinutes(1));
        }
        if (forgotPassword == null) {
            forgotPassword = new RateLimit(3, Duration.ofMinutes(10));
        }
        if (resetPassword == null) {
            resetPassword = new RateLimit(5, Duration.ofMinutes(10));
        }
        if (qrConfirm == null) {
            qrConfirm = new RateLimit(10, Duration.ofMinutes(1));
        }
        if (invitation == null) {
            invitation = new RateLimit(20, Duration.ofMinutes(1));
        }
        if (order == null) {
            order = new RateLimit(10, Duration.ofMinutes(1));
        }
    }

    public enum FailurePolicy {
        FAIL_OPEN,
        FAIL_CLOSED
    }

    public record RateLimit(int permits, Duration window) {
    }
}

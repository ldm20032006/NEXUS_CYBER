package demo.server.common.resilience;

public enum IdempotencyDecisionType {
    STARTED,
    IN_PROGRESS,
    REPLAY,
    FINGERPRINT_MISMATCH
}

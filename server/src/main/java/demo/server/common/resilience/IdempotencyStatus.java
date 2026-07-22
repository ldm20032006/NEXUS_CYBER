package demo.server.common.resilience;

public enum IdempotencyStatus {
    STARTED,
    COMPLETED,
    FAILED
}

package demo.server.common.resilience;

public record IdempotencyDecision(
        IdempotencyDecisionType type,
        IdempotencyRecord record
) {
    public boolean mayProceed() {
        return type == IdempotencyDecisionType.STARTED;
    }
}

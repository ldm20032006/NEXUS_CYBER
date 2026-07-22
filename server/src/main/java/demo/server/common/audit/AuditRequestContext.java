package demo.server.common.audit;

public record AuditRequestContext(
        String ipAddress,
        String userAgent,
        String correlationId
) {
}

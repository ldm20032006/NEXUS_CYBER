package demo.server.common.audit;

import demo.server.common.enums.AuditAction;

import java.util.UUID;

public record AuditRecordCommand(
        UUID actorId,
        String actorRole,
        UUID branchId,
        AuditAction action,
        String resourceType,
        String resourceId,
        Object beforeData,
        Object afterData,
        String ipAddress,
        String userAgent,
        String correlationId
) {
}

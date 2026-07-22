package demo.server.dto.audit;

import demo.server.common.enums.AuditAction;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorRole,
        UUID branchId,
        AuditAction action,
        String resourceType,
        String resourceId,
        String beforeData,
        String afterData,
        String ipAddress,
        String userAgent,
        String correlationId,
        Instant timestamp
) {
}

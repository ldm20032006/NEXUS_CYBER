package demo.server.dto.iot;

import demo.server.common.enums.AlertSeverity;
import demo.server.common.enums.AlertStatus;

import java.time.Instant;
import java.util.UUID;

public record DeviceAlertResponse(
        UUID id,
        UUID deviceId,
        UUID branchId,
        UUID stationId,
        String alertCode,
        String title,
        String description,
        AlertSeverity severity,
        AlertStatus status,
        UUID assignedStaffId,
        UUID acknowledgedById,
        Instant acknowledgedAt,
        UUID resolvedById,
        Instant resolvedAt,
        Instant closedAt,
        boolean criticalMechanicalLock,
        String note
) {
}

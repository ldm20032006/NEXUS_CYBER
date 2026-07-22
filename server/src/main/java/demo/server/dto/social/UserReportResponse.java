package demo.server.dto.social;

import demo.server.common.enums.ModerationActionType;
import demo.server.common.enums.UserReportStatus;

import java.time.Instant;
import java.util.UUID;

public record UserReportResponse(
        UUID id,
        UUID reportedUserId,
        UUID branchId,
        String reason,
        String context,
        UserReportStatus status,
        ModerationActionType moderationAction,
        String moderationNote,
        Instant reportedAt,
        Instant moderatedAt
) {
}

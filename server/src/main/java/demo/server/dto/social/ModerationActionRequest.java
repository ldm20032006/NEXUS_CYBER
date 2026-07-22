package demo.server.dto.social;

import demo.server.common.enums.ModerationActionType;
import demo.server.common.enums.UserReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationActionRequest(
        @NotNull UserReportStatus status,
        @NotNull ModerationActionType action,
        @Size(max = 1000) String note
) {
}

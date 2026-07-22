package demo.server.dto.branch;

import demo.server.common.enums.BranchStatus;

import java.time.LocalTime;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        String code,
        String name,
        String address,
        String timezone,
        BranchStatus status,
        boolean paymentEnabled,
        String paymentPolicy,
        LocalTime operatingStartTime,
        LocalTime operatingEndTime
) {
}

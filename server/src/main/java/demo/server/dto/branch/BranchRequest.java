package demo.server.dto.branch;

import demo.server.common.enums.BranchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record BranchRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String timezone,
        BranchStatus status,
        boolean paymentEnabled,
        @NotBlank @Size(max = 50) String paymentPolicy,
        @NotNull LocalTime operatingStartTime,
        @NotNull LocalTime operatingEndTime
) {
}

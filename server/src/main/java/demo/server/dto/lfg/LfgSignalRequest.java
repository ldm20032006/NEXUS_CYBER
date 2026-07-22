package demo.server.dto.lfg;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LfgSignalRequest(
        @NotNull UUID gameId,
        UUID rankId,
        UUID roleId,
        UUID zoneId,
        @NotNull @Min(2) @Max(10) Integer targetMembers,
        @Size(max = 1000) String message
) {
}

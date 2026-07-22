package demo.server.dto.gamer;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GamerProfileRequest(
        @Size(max = 120) String nickname,
        @Size(max = 500) String avatarUrl,
        LocalDate dateOfBirth,
        Integer heightCm,
        Integer weightKg,
        Boolean nightMode,
        @Size(max = 1000) String bio
) {
}

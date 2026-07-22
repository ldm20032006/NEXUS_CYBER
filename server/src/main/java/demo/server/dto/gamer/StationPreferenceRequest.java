package demo.server.dto.gamer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record StationPreferenceRequest(
        @Min(60) @Max(120) Integer deskHeightCm,
        @Min(90) @Max(145) Integer chairAngleDegree,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String rgbColor,
        @Min(0) @Max(100) Integer brightness,
        @Min(200) @Max(32000) Integer mouseDpi,
        Boolean nightMode
) {
}

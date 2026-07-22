package demo.server.dto.gamer;

import java.util.UUID;

public record StationPreferenceResponse(
        UUID id,
        UUID userId,
        Integer deskHeightCm,
        Integer chairAngleDegree,
        String rgbColor,
        Integer brightness,
        Integer mouseDpi,
        Boolean nightMode
) {
}

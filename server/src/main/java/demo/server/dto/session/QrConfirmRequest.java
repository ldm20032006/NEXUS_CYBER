package demo.server.dto.session;

import jakarta.validation.constraints.NotBlank;

public record QrConfirmRequest(
        @NotBlank String nonce
) {
}

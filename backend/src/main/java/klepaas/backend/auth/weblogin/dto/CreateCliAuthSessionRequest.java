package klepaas.backend.auth.weblogin.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCliAuthSessionRequest(
        @NotBlank String clientName,
        @NotBlank String hostname,
        @NotBlank String platform,
        @NotBlank String cliVersion
) {
}

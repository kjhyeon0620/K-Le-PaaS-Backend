package klepaas.backend.auth.weblogin.dto;

import jakarta.validation.constraints.NotBlank;

public record ExchangeCliAuthSessionRequest(
        @NotBlank String userCode
) {
}

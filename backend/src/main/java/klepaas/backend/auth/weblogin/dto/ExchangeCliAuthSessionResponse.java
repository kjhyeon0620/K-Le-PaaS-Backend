package klepaas.backend.auth.weblogin.dto;

import klepaas.backend.auth.token.dto.CliAccessTokenResponse;

public record ExchangeCliAuthSessionResponse(
        String token,
        CliAccessTokenResponse metadata
) {
}

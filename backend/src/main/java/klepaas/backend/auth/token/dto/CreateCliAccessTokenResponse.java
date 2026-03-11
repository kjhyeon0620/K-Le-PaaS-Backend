package klepaas.backend.auth.token.dto;

public record CreateCliAccessTokenResponse(
        String token,
        CliAccessTokenResponse metadata
) {
}

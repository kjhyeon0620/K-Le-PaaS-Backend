package klepaas.backend.auth.token.dto;

import klepaas.backend.auth.token.entity.CliAccessToken;

import java.time.LocalDateTime;

public record CliAccessTokenResponse(
        Long id,
        String name,
        String tokenPrefix,
        LocalDateTime expiresAt,
        LocalDateTime lastUsedAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt
) {
    public static CliAccessTokenResponse from(CliAccessToken entity) {
        return new CliAccessTokenResponse(
                entity.getId(),
                entity.getName(),
                entity.getTokenPrefix(),
                entity.getExpiresAt(),
                entity.getLastUsedAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}

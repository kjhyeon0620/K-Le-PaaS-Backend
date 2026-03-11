package klepaas.backend.auth.token.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateCliAccessTokenRequest(
        @NotBlank String name,
        @Min(1) @Max(365) int expiresInDays
) {
}

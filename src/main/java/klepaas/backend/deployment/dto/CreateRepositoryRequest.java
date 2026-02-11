package klepaas.backend.deployment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import klepaas.backend.deployment.entity.CloudVendor;

public record CreateRepositoryRequest(
        @NotBlank String owner,
        @NotBlank String repoName,
        @NotBlank String gitUrl,
        @NotNull CloudVendor cloudVendor
) {
}

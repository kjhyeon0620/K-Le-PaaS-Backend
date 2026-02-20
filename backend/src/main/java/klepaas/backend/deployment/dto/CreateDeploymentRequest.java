package klepaas.backend.deployment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeploymentRequest(
        @NotNull Long repositoryId,
        @NotBlank String branchName,
        @NotBlank String commitHash
) {
}

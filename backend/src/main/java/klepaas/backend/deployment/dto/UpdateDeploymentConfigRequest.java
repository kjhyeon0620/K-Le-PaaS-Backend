package klepaas.backend.deployment.dto;

import jakarta.validation.constraints.Min;

import java.util.Map;

public record UpdateDeploymentConfigRequest(
        @Min(0) int minReplicas,
        @Min(1) int maxReplicas,
        Map<String, String> envVars,
        @Min(1) int containerPort,
        String domainUrl
) {
}

package klepaas.backend.deployment.dto;

import klepaas.backend.deployment.entity.DeploymentConfig;

import java.util.Map;

public record DeploymentConfigResponse(
        Long id,
        Long repositoryId,
        int minReplicas,
        int maxReplicas,
        Map<String, String> envVars,
        int containerPort,
        String domainUrl
) {
    public static DeploymentConfigResponse from(DeploymentConfig entity) {
        return new DeploymentConfigResponse(
                entity.getId(),
                entity.getSourceRepository().getId(),
                entity.getMinReplicas(),
                entity.getMaxReplicas(),
                entity.getEnvVars(),
                entity.getContainerPort(),
                entity.getDomainUrl()
        );
    }
}

package klepaas.backend.deployment.dto;

import java.util.List;

public record DeploymentLogResponse(
        Long deploymentId,
        List<String> logs
) {
}

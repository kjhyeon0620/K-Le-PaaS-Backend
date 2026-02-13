package klepaas.backend.infra.ncp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BuildTriggerResponse(
        String buildId,
        String projectId
) {}

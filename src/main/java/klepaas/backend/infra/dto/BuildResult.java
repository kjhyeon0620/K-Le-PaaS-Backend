package klepaas.backend.infra.dto;

public record BuildResult(
        String externalBuildId,
        String trackingUrl
) {}

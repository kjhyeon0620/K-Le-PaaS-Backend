package klepaas.backend.infra.ncp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BuildStatusResponse(
        String buildId,
        String status,       // READY, RUNNING, SUCCEEDED, FAILED, CANCELED
        String imageUri,
        String statusMessage
) {

    public boolean isCompleted() {
        return "SUCCEEDED".equals(status) || "FAILED".equals(status) || "CANCELED".equals(status);
    }

    public boolean isSuccess() {
        return "SUCCEEDED".equals(status);
    }
}

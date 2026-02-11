package klepaas.backend.global.dto;

public record ErrorResponse(
        String error,
        String code,
        String message,
        String path
) {
}

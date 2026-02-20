package klepaas.backend.global.controller;

import klepaas.backend.global.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @Value("${app.version:0.0.1-SNAPSHOT}")
    private String appVersion;

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }

    @GetMapping("/version")
    public ApiResponse<Map<String, String>> version() {
        return ApiResponse.success(Map.of(
                "version", appVersion,
                "java", System.getProperty("java.version")
        ));
    }
}

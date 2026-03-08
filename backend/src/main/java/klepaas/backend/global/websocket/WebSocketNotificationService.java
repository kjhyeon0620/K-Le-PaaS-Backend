package klepaas.backend.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final DeploymentWebSocketHandler handler;

    public void sendDeploymentUpdate(Long deploymentId, Long userId, String stage, String status, int progress, String message) {
        if (userId == null) {
            log.debug("WebSocket notification skipped: userId is null, deploymentId={}", deploymentId);
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "deployment_update");
        payload.put("deployment_id", String.valueOf(deploymentId));
        payload.put("user_id", String.valueOf(userId));
        payload.put("stage", stage);
        payload.put("status", status);
        payload.put("progress", progress);
        payload.put("message", message);
        payload.put("timestamp", Instant.now().toString());

        handler.sendToUser(String.valueOf(userId), payload);
    }
}

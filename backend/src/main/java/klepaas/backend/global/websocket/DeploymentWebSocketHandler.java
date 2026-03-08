package klepaas.backend.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeploymentWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 5000;
    private static final int BUFFER_SIZE_LIMIT = 64 * 1024;

    private final ObjectMapper objectMapper;

    /** userId → Set<WebSocketSession> (thread-safe sessions) */
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    /** sessionId → userId (역방향 조회) */
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = (String) session.getAttributes().get("userId");
        if (userId != null) {
            WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT);
            sessionUserMap.put(session.getId(), userId);
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(safeSession);
            log.info("WebSocket connected: sessionId={}, userId={}", session.getId(), userId);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) payload.get("type");

        if ("ping".equals(type)) {
            Map<String, Object> pong = new LinkedHashMap<>();
            pong.put("type", "pong");
            pong.put("timestamp", Instant.now().toString());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
        }
        // subscribe 메시지는 무시 — userId는 핸드셰이크 시 JWT에서 서버가 직접 추출
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            // 원자적으로 세션 제거: 비어있으면 맵에서 제거
            userSessions.computeIfPresent(userId, (k, sessions) -> {
                sessions.removeIf(s -> s.getId().equals(session.getId()));
                return sessions.isEmpty() ? null : sessions;
            });
        }
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error: sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    public void sendToUser(String userId, Object message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        // ConcurrentWebSocketSessionDecorator handles thread-safety
                        session.sendMessage(textMessage);
                    } catch (Exception e) {
                        log.warn("Failed to send WebSocket message: sessionId={}, error={}", session.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize WebSocket message", e);
        }
    }
}

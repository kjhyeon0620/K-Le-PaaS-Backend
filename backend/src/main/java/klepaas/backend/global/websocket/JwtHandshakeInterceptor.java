package klepaas.backend.global.websocket;

import klepaas.backend.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        String token = extractToken(query);

        if (token == null) {
            log.warn("WebSocket handshake rejected: no token in query string");
            return false;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("WebSocket handshake rejected: invalid JWT token");
            return false;
        }

        Long userId = jwtTokenProvider.getUserId(token);
        attributes.put("userId", userId.toString());
        log.debug("WebSocket handshake accepted: userId={}", userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(String query) {
        if (query == null || query.isBlank()) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                String value = param.substring("token=".length());
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }
}

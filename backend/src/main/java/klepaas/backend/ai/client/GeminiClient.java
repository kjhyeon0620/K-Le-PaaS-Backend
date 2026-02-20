package klepaas.backend.ai.client;

import klepaas.backend.ai.client.dto.GeminiRequest;
import klepaas.backend.ai.client.dto.GeminiResponse;
import klepaas.backend.ai.exception.AiProcessingException;
import klepaas.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient geminiRestClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            @Value("${gemini.api.key}") String apiKey,
            @Value("${gemini.api.model}") String model
    ) {
        this.geminiRestClient = geminiRestClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public GeminiResponse generate(GeminiRequest request) {
        String uri = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        return geminiRestClient.post()
                .uri(uri)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("Gemini API 호출 실패: status={}", res.getStatusCode());
                    throw new AiProcessingException(ErrorCode.AI_API_ERROR,
                            "Gemini API 호출 실패: " + res.getStatusCode());
                })
                .body(GeminiResponse.class);
    }
}

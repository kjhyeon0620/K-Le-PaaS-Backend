package klepaas.backend.ai.client.dto;

import java.util.List;

public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig
) {
    public record Content(
            String role,
            List<Part> parts
    ) {
    }

    public record Part(
            String text
    ) {
    }

    public record GenerationConfig(
            double temperature,
            int maxOutputTokens
    ) {
    }

    public static GeminiRequest of(String systemPrompt, String userMessage) {
        return new GeminiRequest(
                List.of(
                        new Content("user", List.of(
                                new Part(systemPrompt + "\n\n사용자 입력: " + userMessage)
                        ))
                ),
                new GenerationConfig(0.1, 1024)
        );
    }

    public static GeminiRequest withHistory(String systemPrompt, List<Content> history, String userMessage) {
        var contents = new java.util.ArrayList<>(history);
        contents.add(new Content("user", List.of(
                new Part(systemPrompt + "\n\n사용자 입력: " + userMessage)
        )));
        return new GeminiRequest(contents, new GenerationConfig(0.1, 1024));
    }
}

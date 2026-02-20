package klepaas.backend.ai.dto;

import klepaas.backend.ai.entity.Intent;
import klepaas.backend.ai.entity.RiskLevel;

public record NlpCommandResponse(
        Long commandLogId,
        Intent intent,
        String message,
        String result,
        RiskLevel riskLevel,
        boolean requiresConfirmation,
        String sessionId
) {
}

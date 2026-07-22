package demo.server.dto.lobby;

public record VoiceWebhookResponse(
        String eventId,
        String eventType,
        boolean processed
) {
}

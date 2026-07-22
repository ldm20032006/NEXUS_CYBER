package demo.server.service.lfg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.LobbyMemberStatus;
import demo.server.common.enums.LobbyStatus;
import demo.server.common.enums.VoiceChannelStatus;
import demo.server.common.resilience.IdempotencyDecision;
import demo.server.common.resilience.IdempotencyDecisionType;
import demo.server.common.resilience.IdempotencyService;
import demo.server.common.resilience.ResilienceKeys;
import demo.server.common.security.CurrentUserProvider;
import demo.server.dto.lobby.VoiceTokenResponse;
import demo.server.dto.lobby.VoiceWebhookResponse;
import demo.server.entity.lobby.Lobby;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.ConcurrencyConflictException;
import demo.server.exception.ForbiddenException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.lobby.LobbyMemberRepository;
import demo.server.repository.lobby.LobbyRepository;
import demo.server.voice.VoiceChannel;
import demo.server.voice.VoiceProviderException;
import demo.server.voice.VoiceProviderPort;
import demo.server.voice.VoiceProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class VoiceService {

    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final LobbyRepository lobbyRepository;
    private final LobbyMemberRepository memberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final VoiceProviderPort voiceProvider;
    private final VoiceProperties properties;
    private final IdempotencyService idempotencyService;
    private final ResilienceKeys resilienceKeys;
    private final ObjectMapper objectMapper;

    public VoiceService(LobbyRepository lobbyRepository, LobbyMemberRepository memberRepository,
                        CurrentUserProvider currentUserProvider, VoiceProviderPort voiceProvider,
                        VoiceProperties properties, IdempotencyService idempotencyService,
                        ResilienceKeys resilienceKeys, ObjectMapper objectMapper) {
        this.lobbyRepository = lobbyRepository;
        this.memberRepository = memberRepository;
        this.currentUserProvider = currentUserProvider;
        this.voiceProvider = voiceProvider;
        this.properties = properties;
        this.idempotencyService = idempotencyService;
        this.resilienceKeys = resilienceKeys;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public VoiceTokenResponse issueToken(UUID lobbyId) {
        UUID userId = currentUserProvider.currentUserId()
                .orElseThrow(() -> new UnauthorizedException("Authentication is required"));
        Lobby lobby = lobbyRepository.findByIdForUpdate(lobbyId)
                .filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lobby not found"));
        if (lobby.getStatus() == LobbyStatus.CLOSED) {
            throw new InvalidVoiceStateException("Lobby is closed");
        }
        memberRepository.findByLobby_IdAndUser_IdAndStatus(lobbyId, userId, LobbyMemberStatus.ACTIVE)
                .orElseThrow(() -> new ForbiddenException("Voice is available to active lobby members only"));
        try {
            ensureChannel(lobby);
            VoiceProviderPort.VoiceToken token = voiceProvider.issueToken(lobby.getVoiceChannelId(), lobbyId, userId, properties.tokenTtl());
            return new VoiceTokenResponse(lobbyId, userId, voiceProvider.providerName(), lobby.getVoiceChannelId(),
                    token.token(), token.expiresAt(), VoiceChannelStatus.ACTIVE);
        } catch (VoiceProviderException ex) {
            lobby.setVoiceStatus(VoiceChannelStatus.VOICE_UNAVAILABLE);
            return new VoiceTokenResponse(lobbyId, userId, voiceProvider.providerName(), lobby.getVoiceChannelId(),
                    null, null, VoiceChannelStatus.VOICE_UNAVAILABLE);
        }
    }

    @Transactional
    public VoiceWebhookResponse handleWebhook(String rawBody, String timestampHeader, String signatureHeader) {
        verifyWebhook(rawBody, timestampHeader, signatureHeader);
        JsonNode payload = parse(rawBody);
        String eventId = requiredText(payload, "eventId");
        String eventType = requiredText(payload, "eventType");
        String fingerprint = eventId + ":" + eventType + ":" + sha256(rawBody);
        String replayKey = resilienceKeys.idempotency("voice-webhook", eventId);
        IdempotencyDecision decision = idempotencyService.begin(replayKey, fingerprint, IDEMPOTENCY_TTL);
        if (decision.type() == IdempotencyDecisionType.FINGERPRINT_MISMATCH) {
            throw new ConcurrencyConflictException("Voice webhook fingerprint mismatch");
        }
        if (decision.type() == IdempotencyDecisionType.IN_PROGRESS) {
            throw new ConcurrencyConflictException("Voice webhook is already processing");
        }
        if (decision.type() == IdempotencyDecisionType.REPLAY) {
            return new VoiceWebhookResponse(eventId, eventType, false);
        }
        idempotencyService.complete(replayKey, 200);
        return new VoiceWebhookResponse(eventId, eventType, true);
    }

    void closeChannel(Lobby lobby) {
        if (!StringUtils.hasText(lobby.getVoiceChannelId()) || lobby.getVoiceStatus() == VoiceChannelStatus.CLOSED) {
            lobby.setVoiceStatus(VoiceChannelStatus.CLOSED);
            return;
        }
        try {
            voiceProvider.closeChannel(lobby.getVoiceChannelId());
            lobby.setVoiceStatus(VoiceChannelStatus.CLOSED);
        } catch (VoiceProviderException ex) {
            lobby.setVoiceStatus(VoiceChannelStatus.VOICE_UNAVAILABLE);
        }
    }

    private void ensureChannel(Lobby lobby) {
        if (StringUtils.hasText(lobby.getVoiceChannelId()) && lobby.getVoiceStatus() == VoiceChannelStatus.ACTIVE) {
            return;
        }
        VoiceChannel channel = voiceProvider.createChannel(lobby.getId(), lobby.getName());
        lobby.setVoiceProvider(channel.provider());
        lobby.setVoiceChannelId(channel.channelId());
        lobby.setVoiceStatus(VoiceChannelStatus.ACTIVE);
    }

    private void verifyWebhook(String rawBody, String timestampHeader, String signatureHeader) {
        if (!StringUtils.hasText(properties.webhookSecret())) {
            throw new UnauthorizedException("Voice webhook secret is not configured");
        }
        Instant timestamp = parseTimestamp(timestampHeader);
        Instant now = Instant.now();
        if (timestamp.isBefore(now.minus(properties.webhookReplayWindow()))
                || timestamp.isAfter(now.plus(Duration.ofMinutes(1)))) {
            throw new UnauthorizedException("Voice webhook timestamp is outside replay window");
        }
        String expected = hmacSha256Hex(properties.webhookSecret(), timestampHeader + "." + rawBody);
        if (!constantTimeEquals(expected, signatureHeader)) {
            throw new UnauthorizedException("Voice webhook signature is invalid");
        }
    }

    private Instant parseTimestamp(String value) {
        if (!StringUtils.hasText(value)) {
            throw new UnauthorizedException("Voice webhook timestamp is required");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(value));
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Voice webhook timestamp is invalid");
            }
        }
    }

    private JsonNode parse(String rawBody) {
        try {
            return objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BusinessRuleException("Voice webhook payload is invalid");
        }
    }

    private String requiredText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.asText().isBlank()) {
            throw new BusinessRuleException("Voice webhook field is required: " + field);
        }
        return value.asText();
    }

    private String hmacSha256Hex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessRuleException("Voice signature verification failed");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (!StringUtils.hasText(actual)) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessRuleException("Voice webhook fingerprint failed");
        }
    }

    static class InvalidVoiceStateException extends BusinessRuleException {
        InvalidVoiceStateException(String message) {
            super(message);
        }
    }
}

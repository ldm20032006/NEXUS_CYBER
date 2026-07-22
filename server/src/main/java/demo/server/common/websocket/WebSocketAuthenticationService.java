package demo.server.common.websocket;

import demo.server.common.enums.UserStatus;
import demo.server.common.security.JwtService;
import demo.server.common.security.TokenHashService;
import demo.server.entity.branch.StationCredential;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.branch.StationCredentialRepository;
import demo.server.repository.branch.StationRepository;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WebSocketAuthenticationService {

    public static final String STATION_ID_HEADER = "X-Station-Id";
    public static final String STATION_SECRET_HEADER = "X-Station-Secret";

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final StationRepository stationRepository;
    private final StationCredentialRepository credentialRepository;
    private final TokenHashService tokenHashService;

    public WebSocketAuthenticationService(JwtService jwtService, AppUserRepository appUserRepository,
                                          StationRepository stationRepository,
                                          StationCredentialRepository credentialRepository,
                                          TokenHashService tokenHashService) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.stationRepository = stationRepository;
        this.credentialRepository = credentialRepository;
        this.tokenHashService = tokenHashService;
    }

    public WebSocketPrincipal authenticate(StompHeaderAccessor accessor) {
        return authenticateUser(accessor).or(() -> authenticateStation(accessor))
                .orElseThrow(() -> new UnauthorizedException("WebSocket authentication is required"));
    }

    private Optional<WebSocketPrincipal> authenticateUser(StompHeaderAccessor accessor) {
        String authorization = firstHeader(accessor, "Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        UUID userId = jwtService.parseSubject(authorization.substring(7));
        return appUserRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> WebSocketPrincipal.user(
                        user.getId(),
                        user.getBranch() == null ? null : user.getBranch().getId(),
                        user.getRoles().stream().map(role -> role.getCode()).collect(Collectors.toSet())));
    }

    private Optional<WebSocketPrincipal> authenticateStation(StompHeaderAccessor accessor) {
        String stationIdValue = firstHeader(accessor, STATION_ID_HEADER);
        String stationSecret = firstHeader(accessor, STATION_SECRET_HEADER);
        if (!StringUtils.hasText(stationIdValue) || !StringUtils.hasText(stationSecret)) {
            return Optional.empty();
        }
        UUID stationId = UUID.fromString(stationIdValue);
        StationCredential credential = credentialRepository.findFirstByStation_IdAndRevokedAtIsNullOrderByIssuedAtDesc(stationId)
                .orElseThrow(() -> new UnauthorizedException("Station credential is invalid"));
        if (!tokenHashService.hash(stationSecret).equals(credential.getSecretHash())) {
            throw new UnauthorizedException("Station credential is invalid");
        }
        if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Station credential has expired");
        }
        credential.setLastUsedAt(Instant.now());
        return stationRepository.findById(stationId)
                .filter(station -> !station.isDeleted())
                .map(station -> WebSocketPrincipal.station(station.getId(), station.getBranch().getId()));
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        return accessor.getFirstNativeHeader(name);
    }
}

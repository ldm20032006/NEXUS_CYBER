package demo.server.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.server.common.enums.RoleCode;
import demo.server.entity.auth.AppUser;
import demo.server.exception.TokenException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(AppUser user) {
        Instant now = Instant.now();
        Set<RoleCode> roles = user.getRoles().stream().map(role -> role.getCode()).collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("roles", roles.stream().map(Enum::name).toList());
        claims.put("permissions", permissions);
        claims.put("branchId", user.getBranch() == null ? null : user.getBranch().getId().toString());
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plus(properties.accessTokenTtl()).getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        return sign(claims);
    }

    public UUID parseSubject(String token) {
        JsonNode payload = parsePayload(token);
        long exp = payload.path("exp").asLong(0);
        if (exp <= Instant.now().getEpochSecond()) {
            throw new TokenException("Access token has expired");
        }
        String subject = payload.path("sub").asText(null);
        if (!StringUtils.hasText(subject)) {
            throw new TokenException("Access token subject is missing");
        }
        return UUID.fromString(subject);
    }

    private String sign(Map<String, Object> claims) {
        requireSecret();
        try {
            String header = ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String signature = hmac(header + "." + payload);
            return header + "." + payload + "." + signature;
        } catch (JsonProcessingException ex) {
            throw new TokenException("Unable to create access token");
        }
    }

    private JsonNode parsePayload(String token) {
        requireSecret();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new TokenException("Access token format is invalid");
        }
        String expected = hmac(parts[0] + "." + parts[1]);
        if (!MessageDigestTiming.safeEquals(expected, parts[2])) {
            throw new TokenException("Access token signature is invalid");
        }
        try {
            return objectMapper.readTree(DECODER.decode(parts[1]));
        } catch (Exception ex) {
            throw new TokenException("Access token payload is invalid");
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new TokenException("Unable to verify access token");
        }
    }

    private void requireSecret() {
        if (!StringUtils.hasText(properties.secret()) || properties.secret().length() < 32) {
            throw new TokenException("JWT secret is not configured");
        }
    }

    private static final class MessageDigestTiming {
        private static boolean safeEquals(String left, String right) {
            return java.security.MessageDigest.isEqual(
                    left.getBytes(StandardCharsets.UTF_8),
                    right.getBytes(StandardCharsets.UTF_8));
        }
    }
}

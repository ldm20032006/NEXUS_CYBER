package demo.server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security")
public record AuthTokenProperties(
        Duration refreshTokenTtl,
        Duration passwordResetTokenTtl
) {
}

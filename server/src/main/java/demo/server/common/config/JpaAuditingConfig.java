package demo.server.common.config;

import demo.server.common.security.CurrentUserProvider;
import demo.server.common.time.ClockProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "utcDateTimeProvider", auditorAwareRef = "currentAuditorProvider")
public class JpaAuditingConfig {

    @Bean
    DateTimeProvider utcDateTimeProvider(ClockProvider clockProvider) {
        return () -> Optional.of(clockProvider.now());
    }

    @Bean
    AuditorAware<UUID> currentAuditorProvider(CurrentUserProvider currentUserProvider) {
        return currentUserProvider::currentUserId;
    }
}

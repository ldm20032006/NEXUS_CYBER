package demo.server.service.jobs;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "nexus.jobs")
public record BackgroundJobProperties(
        boolean enabled,
        int batchSize,
        Duration lockTtl,
        Duration qrExpireInterval,
        Duration invitationExpireInterval,
        Duration lfgExpireInterval,
        Duration heartbeatInterval,
        Duration stationHeartbeatTimeout,
        Duration deviceHeartbeatTimeout,
        Duration tokenCleanupInterval,
        Duration notificationRetryInterval,
        Duration retentionInterval,
        Duration notificationRetention,
        Duration chatRetention,
        Duration auditRetention,
        Duration sessionWarningInterval,
        Duration sessionEndingWarningBefore,
        Duration paymentReconciliationInterval
) {
}

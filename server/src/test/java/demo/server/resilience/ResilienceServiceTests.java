package demo.server.resilience;

import demo.server.common.resilience.IdempotencyDecision;
import demo.server.common.resilience.IdempotencyDecisionType;
import demo.server.common.resilience.InMemoryCacheService;
import demo.server.common.resilience.InMemoryDistributedLockService;
import demo.server.common.resilience.InMemoryIdempotencyService;
import demo.server.common.resilience.InMemoryOnlineStateService;
import demo.server.common.resilience.InMemoryRateLimitService;
import demo.server.common.resilience.LockHandle;
import demo.server.common.resilience.RateLimitDecision;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ResilienceServiceTests {

    @Test
    void cacheHonorsTtl() throws Exception {
        InMemoryCacheService cache = new InMemoryCacheService();
        cache.put("nexus:test:cache", "value", Duration.ofMillis(50));

        assertThat(cache.get("nexus:test:cache", String.class)).contains("value");
        Thread.sleep(80);
        assertThat(cache.get("nexus:test:cache", String.class)).isEmpty();
    }

    @Test
    void rateLimitBlocksAfterWindowPermits() {
        InMemoryRateLimitService rateLimitService = new InMemoryRateLimitService();

        RateLimitDecision first = rateLimitService.consume("nexus:test:rl", 2, Duration.ofMinutes(1));
        RateLimitDecision second = rateLimitService.consume("nexus:test:rl", 2, Duration.ofMinutes(1));
        RateLimitDecision third = rateLimitService.consume("nexus:test:rl", 2, Duration.ofMinutes(1));

        assertThat(first.allowed()).isTrue();
        assertThat(second.allowed()).isTrue();
        assertThat(third.allowed()).isFalse();
    }

    @Test
    void distributedLockAllowsSingleOwnerUntilReleasedOrExpired() throws Exception {
        InMemoryDistributedLockService lockService = new InMemoryDistributedLockService();

        Optional<LockHandle> first = lockService.tryAcquire("nexus:test:lock", Duration.ofMillis(80));
        Optional<LockHandle> second = lockService.tryAcquire("nexus:test:lock", Duration.ofMillis(80));

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        first.get().close();
        assertThat(lockService.tryAcquire("nexus:test:lock", Duration.ofMillis(80))).isPresent();

        Optional<LockHandle> expiring = lockService.tryAcquire("nexus:test:lock-expiring", Duration.ofMillis(30));
        assertThat(expiring).isPresent();
        Thread.sleep(60);
        assertThat(lockService.tryAcquire("nexus:test:lock-expiring", Duration.ofMillis(30))).isPresent();
    }

    @Test
    void idempotencyTracksFingerprintAndStatus() {
        InMemoryIdempotencyService service = new InMemoryIdempotencyService();

        IdempotencyDecision first = service.begin("nexus:test:idem", "fingerprint-a", Duration.ofMinutes(5));
        IdempotencyDecision inProgress = service.begin("nexus:test:idem", "fingerprint-a", Duration.ofMinutes(5));
        IdempotencyDecision mismatch = service.begin("nexus:test:idem", "fingerprint-b", Duration.ofMinutes(5));
        service.complete("nexus:test:idem", 201);
        IdempotencyDecision replay = service.begin("nexus:test:idem", "fingerprint-a", Duration.ofMinutes(5));

        assertThat(first.type()).isEqualTo(IdempotencyDecisionType.STARTED);
        assertThat(inProgress.type()).isEqualTo(IdempotencyDecisionType.IN_PROGRESS);
        assertThat(mismatch.type()).isEqualTo(IdempotencyDecisionType.FINGERPRINT_MISMATCH);
        assertThat(replay.type()).isEqualTo(IdempotencyDecisionType.REPLAY);
        assertThat(replay.record().statusCode()).isEqualTo(201);
    }

    @Test
    void onlineStateExpires() throws Exception {
        InMemoryOnlineStateService onlineStateService = new InMemoryOnlineStateService();
        onlineStateService.markOnline("nexus:test:online:user", Duration.ofMillis(40));

        assertThat(onlineStateService.isOnline("nexus:test:online:user")).isTrue();
        Thread.sleep(70);
        assertThat(onlineStateService.isOnline("nexus:test:online:user")).isFalse();
    }
}

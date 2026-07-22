package demo.server.entity.notification;

import demo.server.common.entity.BaseEntity;
import demo.server.entity.auth.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "push_subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_push_subscription_endpoint_hash", columnNames = "endpoint_hash")
)
public class PushSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "endpoint_hash", nullable = false, length = 128, unique = true)
    private String endpointHash;

    @Column(nullable = false, length = 2000)
    private String endpoint;

    @Column(nullable = false, length = 500)
    private String p256dh;

    @Column(nullable = false, length = 500)
    private String auth;

    @Column(length = 100)
    private String userAgent;

    private Instant lastUsedAt;
}

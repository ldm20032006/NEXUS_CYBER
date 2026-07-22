package demo.server.entity.branch;

import demo.server.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "station_credentials")
public class StationCredential extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false, length = 512, unique = true)
    private String secretHash;

    @Column(nullable = false)
    private Instant issuedAt;

    private Instant expiresAt;

    private Instant revokedAt;

    private Instant lastUsedAt;

    @Column(length = 255)
    private String revokeReason;
}

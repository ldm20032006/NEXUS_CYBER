package demo.server.entity.audit;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.AuditAction;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Column(length = 50)
    private String actorRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(nullable = false, length = 100)
    private String resourceType;

    @Column(length = 100)
    private String resourceId;

    @Column(length = 4000)
    private String beforeData;

    @Column(length = 4000)
    private String afterData;

    @Column(length = 100)
    private String ipAddress;

    @Column(length = 255)
    private String userAgent;

    @Column(length = 100)
    private String correlationId;

    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @PreUpdate
    @PreRemove
    private void preventMutation() {
        throw new UnsupportedOperationException("AuditLog is append-only");
    }
}

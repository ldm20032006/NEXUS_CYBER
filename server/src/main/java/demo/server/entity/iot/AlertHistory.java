package demo.server.entity.iot;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.AlertStatus;
import demo.server.entity.auth.AppUser;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alert_history")
public class AlertHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private DeviceAlert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private AlertStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private AlertStatus toStatus;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 1000)
    private String note;

    @PreUpdate
    @PreRemove
    void preventMutation() {
        throw new UnsupportedOperationException("AlertHistory is append-only");
    }
}

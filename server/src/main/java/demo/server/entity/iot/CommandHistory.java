package demo.server.entity.iot;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.DeviceCommandStatus;
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
@Table(name = "command_history")
public class CommandHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "command_id", nullable = false)
    private DeviceCommand command;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private DeviceCommandStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private DeviceCommandStatus toStatus;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(length = 1000)
    private String note;

    @PreUpdate
    @PreRemove
    void preventMutation() {
        throw new UnsupportedOperationException("CommandHistory is append-only");
    }
}

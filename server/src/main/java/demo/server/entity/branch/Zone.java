package demo.server.entity.branch;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.ZoneStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "zones",
        uniqueConstraints = @UniqueConstraint(name = "uk_zone_branch_code", columnNames = {"branch_id", "code"})
)
public class Zone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String zoneType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ZoneStatus status = ZoneStatus.ACTIVE;

    private Integer sortOrder;
}

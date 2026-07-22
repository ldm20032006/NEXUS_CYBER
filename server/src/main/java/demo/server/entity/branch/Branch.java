package demo.server.entity.branch;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.BranchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "branches")
public class Branch extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BranchStatus status = BranchStatus.ACTIVE;

    private boolean paymentEnabled = true;

    @Column(nullable = false, length = 50)
    private String paymentPolicy = "PREPAID_OR_WALLET";

    private LocalTime operatingStartTime;

    private LocalTime operatingEndTime;
}

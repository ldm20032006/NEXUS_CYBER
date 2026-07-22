package demo.server.entity.social;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.ModerationActionType;
import demo.server.common.enums.UserReportStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_reports")
public class UserReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private AppUser reportedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(length = 1000)
    private String context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserReportStatus status = UserReportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ModerationActionType moderationAction;

    @Column(length = 1000)
    private String moderationNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private AppUser moderator;

    private Instant reportedAt;

    private Instant moderatedAt;
}

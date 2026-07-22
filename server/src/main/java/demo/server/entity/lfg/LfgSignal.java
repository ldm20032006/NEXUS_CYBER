package demo.server.entity.lfg;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.LfgSignalStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Zone;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.entity.session.PlaySession;
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
@Table(name = "lfg_signals")
public class LfgSignal extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "play_session_id")
    private PlaySession playSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id")
    private GameRank rank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private GameRole role;

    @Column(nullable = false)
    private Integer targetMembers;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LfgSignalStatus status = LfgSignalStatus.ACTIVE;

    @Column(nullable = false)
    private Instant expiresAt;
}

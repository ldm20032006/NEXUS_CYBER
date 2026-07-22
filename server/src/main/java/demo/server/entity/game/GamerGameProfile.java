package demo.server.entity.game;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "gamer_game_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_gamer_game_profile", columnNames = {"user_id", "game_id"})
)
public class GamerGameProfile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(length = 120)
    private String inGameName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id")
    private GameRank rank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_role_id")
    private GameRole preferredRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "secondary_role_id")
    private GameRole secondaryRole;

    @Column(length = 1000)
    private String playStyle;

    @Column(length = 1000)
    private String shortDescription;

    private Boolean visibleOnRadar = Boolean.TRUE;
}

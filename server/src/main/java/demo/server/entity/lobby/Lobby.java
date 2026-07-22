package demo.server.entity.lobby;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.LobbyStatus;
import demo.server.common.enums.VoiceChannelStatus;
import demo.server.entity.auth.AppUser;
import demo.server.entity.branch.Branch;
import demo.server.entity.branch.Zone;
import demo.server.entity.game.Game;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lobbies")
public class Lobby extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private AppUser creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", nullable = false)
    private AppUser leader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer maxMembers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LobbyStatus status = LobbyStatus.OPEN;

    @Column(name = "voice_provider", length = 50)
    private String voiceProvider;

    @Column(name = "voice_channel_id", length = 150)
    private String voiceChannelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "voice_status", nullable = false, length = 30)
    private VoiceChannelStatus voiceStatus = VoiceChannelStatus.DISABLED;
}

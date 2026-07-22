package demo.server.entity.lobby;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.LobbyMemberRole;
import demo.server.common.enums.LobbyMemberStatus;
import demo.server.entity.auth.AppUser;
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

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "lobby_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_lobby_member", columnNames = {"lobby_id", "user_id"})
)
public class LobbyMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LobbyMemberRole role = LobbyMemberRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LobbyMemberStatus status = LobbyMemberStatus.ACTIVE;

    @Column(nullable = false)
    private Instant joinedAt;

    private Instant leftAt;
}

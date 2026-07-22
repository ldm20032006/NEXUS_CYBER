package demo.server.service.game;

import demo.server.dto.game.GameRankResponse;
import demo.server.dto.game.GameResponse;
import demo.server.dto.game.GameRoleResponse;
import demo.server.dto.gamer.GamerGameProfileResponse;
import demo.server.dto.gamer.PublicGamerProfileResponse;
import demo.server.dto.gamer.StationPreferenceResponse;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.entity.game.GamerGameProfile;
import demo.server.entity.gamer.GamerProfile;
import demo.server.entity.gamer.StationPreference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameMapper {

    public GameResponse toGame(Game game, List<GameRank> ranks, List<GameRole> roles) {
        return new GameResponse(game.getId(), game.getSlug(), game.getName(), game.getDescription(), game.getMaxLobbySize(),
                game.getStatus(), ranks.stream().map(this::toRank).toList(), roles.stream().map(this::toRole).toList());
    }

    public GameRankResponse toRank(GameRank rank) {
        return new GameRankResponse(rank.getId(), rank.getGame().getId(), rank.getCode(), rank.getName(), rank.getSortOrder());
    }

    public GameRoleResponse toRole(GameRole role) {
        return new GameRoleResponse(role.getId(), role.getGame().getId(), role.getCode(), role.getName(), role.getSortOrder());
    }

    public PublicGamerProfileResponse toPublicProfile(GamerProfile profile) {
        return new PublicGamerProfileResponse(profile.getUser().getId(), profile.getNickname(), profile.getAvatarUrl(), profile.getBio());
    }

    public GamerGameProfileResponse toGameProfile(GamerGameProfile profile) {
        return new GamerGameProfileResponse(profile.getId(), profile.getUser().getId(), profile.getGame().getId(),
                profile.getGame().getName(), profile.getInGameName(),
                profile.getRank() == null ? null : profile.getRank().getId(),
                profile.getRank() == null ? null : profile.getRank().getName(),
                profile.getPreferredRole() == null ? null : profile.getPreferredRole().getId(),
                profile.getPreferredRole() == null ? null : profile.getPreferredRole().getName(),
                profile.getSecondaryRole() == null ? null : profile.getSecondaryRole().getId(),
                profile.getSecondaryRole() == null ? null : profile.getSecondaryRole().getName(),
                profile.getPlayStyle(), profile.getShortDescription(), profile.getVisibleOnRadar());
    }

    public StationPreferenceResponse toStationPreference(StationPreference preference) {
        return new StationPreferenceResponse(preference.getId(), preference.getUser().getId(), preference.getDeskHeightCm(),
                preference.getChairAngleDegree(), preference.getRgbColor(), preference.getBrightness(),
                preference.getMouseDpi(), preference.getNightMode());
    }
}

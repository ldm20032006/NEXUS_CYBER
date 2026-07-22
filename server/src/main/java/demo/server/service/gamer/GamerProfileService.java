package demo.server.service.gamer;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.GameStatus;
import demo.server.common.security.CurrentUserProvider;
import demo.server.dto.gamer.GamerGameProfileRequest;
import demo.server.dto.gamer.GamerGameProfileResponse;
import demo.server.dto.gamer.GamerProfileRequest;
import demo.server.dto.gamer.PublicGamerProfileResponse;
import demo.server.dto.gamer.StationPreferenceRequest;
import demo.server.dto.gamer.StationPreferenceResponse;
import demo.server.entity.auth.AppUser;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.entity.game.GamerGameProfile;
import demo.server.entity.gamer.GamerProfile;
import demo.server.entity.gamer.StationPreference;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.exception.UnauthorizedException;
import demo.server.repository.auth.AppUserRepository;
import demo.server.repository.game.GameRankRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.game.GameRoleRepository;
import demo.server.repository.game.GamerGameProfileRepository;
import demo.server.repository.gamer.GamerProfileRepository;
import demo.server.repository.gamer.StationPreferenceRepository;
import demo.server.service.game.GameMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class GamerProfileService {

    private final CurrentUserProvider currentUserProvider;
    private final AppUserRepository appUserRepository;
    private final GamerProfileRepository profileRepository;
    private final StationPreferenceRepository stationPreferenceRepository;
    private final GamerGameProfileRepository gameProfileRepository;
    private final GameRepository gameRepository;
    private final GameRankRepository rankRepository;
    private final GameRoleRepository roleRepository;
    private final GameMapper mapper;
    private final AuditRecorder auditRecorder;

    public GamerProfileService(CurrentUserProvider currentUserProvider, AppUserRepository appUserRepository,
                               GamerProfileRepository profileRepository,
                               StationPreferenceRepository stationPreferenceRepository,
                               GamerGameProfileRepository gameProfileRepository, GameRepository gameRepository,
                               GameRankRepository rankRepository, GameRoleRepository roleRepository,
                               GameMapper mapper, AuditRecorder auditRecorder) {
        this.currentUserProvider = currentUserProvider;
        this.appUserRepository = appUserRepository;
        this.profileRepository = profileRepository;
        this.stationPreferenceRepository = stationPreferenceRepository;
        this.gameProfileRepository = gameProfileRepository;
        this.gameRepository = gameRepository;
        this.rankRepository = rankRepository;
        this.roleRepository = roleRepository;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public PublicGamerProfileResponse me() {
        return mapper.toPublicProfile(profile());
    }

    @Transactional
    public PublicGamerProfileResponse updateMe(GamerProfileRequest request) {
        GamerProfile profile = profile();
        PublicGamerProfileResponse before = mapper.toPublicProfile(profile);
        profile.setNickname(request.nickname());
        profile.setAvatarUrl(blankToNull(request.avatarUrl()));
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setNightMode(request.nightMode() == null ? Boolean.FALSE : request.nightMode());
        profile.setBio(request.bio());
        PublicGamerProfileResponse after = mapper.toPublicProfile(profile);
        auditRecorder.record(AuditAction.UPDATE_GAMER_PROFILE, "GamerProfile", profile.getId(), before, after);
        return after;
    }

    @Transactional(readOnly = true)
    public List<GamerGameProfileResponse> gameProfiles() {
        UUID userId = currentUserId();
        return gameProfileRepository.findByUser_Id(userId).stream().filter(profile -> !profile.isDeleted()).map(mapper::toGameProfile).toList();
    }

    @Transactional
    public GamerGameProfileResponse createGameProfile(GamerGameProfileRequest request) {
        UUID userId = currentUserId();
        if (gameProfileRepository.existsByUser_IdAndGame_Id(userId, request.gameId())) {
            throw new DuplicateResourceException("Gamer already has a profile for this game");
        }
        GamerGameProfile profile = new GamerGameProfile();
        profile.setUser(user());
        apply(profile, request);
        GamerGameProfile saved = gameProfileRepository.save(profile);
        auditRecorder.record(AuditAction.CREATE_GAME_PROFILE, "GamerGameProfile", saved.getId(), null, mapper.toGameProfile(saved));
        return mapper.toGameProfile(saved);
    }

    @Transactional
    public GamerGameProfileResponse updateGameProfile(UUID id, GamerGameProfileRequest request) {
        GamerGameProfile profile = ownedGameProfile(id);
        if (!profile.getGame().getId().equals(request.gameId())) {
            throw new BusinessRuleException("Game profile game cannot be changed");
        }
        GamerGameProfileResponse before = mapper.toGameProfile(profile);
        apply(profile, request);
        GamerGameProfileResponse after = mapper.toGameProfile(profile);
        auditRecorder.record(AuditAction.UPDATE_GAME_PROFILE, "GamerGameProfile", id, before, after);
        return after;
    }

    @Transactional
    public void deleteGameProfile(UUID id) {
        GamerGameProfile profile = ownedGameProfile(id);
        profile.softDelete();
        auditRecorder.record(AuditAction.DELETE_GAME_PROFILE, "GamerGameProfile", id, mapper.toGameProfile(profile), "SOFT_DELETED");
    }

    @Transactional(readOnly = true)
    public StationPreferenceResponse stationPreference() {
        return mapper.toStationPreference(stationPreferenceEntity());
    }

    @Transactional
    public StationPreferenceResponse updateStationPreference(StationPreferenceRequest request) {
        StationPreference preference = stationPreferenceEntity();
        StationPreferenceResponse before = mapper.toStationPreference(preference);
        preference.setDeskHeightCm(request.deskHeightCm());
        preference.setChairAngleDegree(request.chairAngleDegree());
        preference.setRgbColor(request.rgbColor() == null ? null : request.rgbColor().toUpperCase());
        preference.setBrightness(request.brightness());
        preference.setMouseDpi(request.mouseDpi());
        preference.setNightMode(request.nightMode() == null ? Boolean.FALSE : request.nightMode());
        StationPreferenceResponse after = mapper.toStationPreference(preference);
        auditRecorder.record(AuditAction.UPDATE_STATION_PREFERENCE, "StationPreference", preference.getId(), before, after);
        return after;
    }

    private void apply(GamerGameProfile profile, GamerGameProfileRequest request) {
        Game game = gameRepository.findById(request.gameId()).filter(item -> !item.isDeleted() && item.getStatus() == GameStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active game not found"));
        profile.setGame(game);
        profile.setInGameName(request.inGameName());
        profile.setRank(resolveRank(game.getId(), request.rankId()));
        profile.setPreferredRole(resolveRole(game.getId(), request.preferredRoleId()));
        profile.setSecondaryRole(resolveRole(game.getId(), request.secondaryRoleId()));
        profile.setPlayStyle(request.playStyle());
        profile.setShortDescription(request.shortDescription());
        profile.setVisibleOnRadar(request.visibleOnRadar() == null ? Boolean.TRUE : request.visibleOnRadar());
    }

    private GameRank resolveRank(UUID gameId, UUID rankId) {
        if (rankId == null) {
            return null;
        }
        GameRank rank = rankRepository.findById(rankId).orElseThrow(() -> new ResourceNotFoundException("Game rank not found"));
        if (!rank.getGame().getId().equals(gameId)) {
            throw new BusinessRuleException("Rank must belong to selected game");
        }
        return rank;
    }

    private GameRole resolveRole(UUID gameId, UUID roleId) {
        if (roleId == null) {
            return null;
        }
        GameRole role = roleRepository.findById(roleId).orElseThrow(() -> new ResourceNotFoundException("Game role not found"));
        if (!role.getGame().getId().equals(gameId)) {
            throw new BusinessRuleException("Role must belong to selected game");
        }
        return role;
    }

    private GamerProfile profile() {
        UUID userId = currentUserId();
        return profileRepository.findByUser_Id(userId).orElseGet(() -> {
            GamerProfile profile = new GamerProfile();
            profile.setUser(user());
            return profileRepository.save(profile);
        });
    }

    private StationPreference stationPreferenceEntity() {
        UUID userId = currentUserId();
        return stationPreferenceRepository.findByUser_Id(userId).orElseGet(() -> {
            StationPreference preference = new StationPreference();
            preference.setUser(user());
            return stationPreferenceRepository.save(preference);
        });
    }

    private GamerGameProfile ownedGameProfile(UUID id) {
        UUID userId = currentUserId();
        GamerGameProfile profile = gameProfileRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Game profile not found"));
        if (!profile.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Game profile not found");
        }
        return profile;
    }

    private AppUser user() {
        return appUserRepository.findById(currentUserId()).orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private UUID currentUserId() {
        return currentUserProvider.currentUserId().orElseThrow(() -> new UnauthorizedException("Authentication is required"));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

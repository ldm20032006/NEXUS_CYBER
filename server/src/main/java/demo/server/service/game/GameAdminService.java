package demo.server.service.game;

import demo.server.common.audit.AuditRecorder;
import demo.server.common.enums.AuditAction;
import demo.server.common.enums.GameStatus;
import demo.server.common.response.PageResponse;
import demo.server.dto.game.GameRankRequest;
import demo.server.dto.game.GameRankResponse;
import demo.server.dto.game.GameRequest;
import demo.server.dto.game.GameResponse;
import demo.server.dto.game.GameRoleRequest;
import demo.server.dto.game.GameRoleResponse;
import demo.server.entity.game.Game;
import demo.server.entity.game.GameRank;
import demo.server.entity.game.GameRole;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.repository.game.GameRankRepository;
import demo.server.repository.game.GameRepository;
import demo.server.repository.game.GameRoleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class GameAdminService {

    private final GameRepository gameRepository;
    private final GameRankRepository rankRepository;
    private final GameRoleRepository roleRepository;
    private final GameMapper mapper;
    private final AuditRecorder auditRecorder;

    public GameAdminService(GameRepository gameRepository, GameRankRepository rankRepository, GameRoleRepository roleRepository,
                            GameMapper mapper, AuditRecorder auditRecorder) {
        this.gameRepository = gameRepository;
        this.rankRepository = rankRepository;
        this.roleRepository = roleRepository;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public GameResponse createGame(GameRequest request) {
        if (gameRepository.existsBySlug(slug(request.slug()))) {
            throw new DuplicateResourceException("Game slug already exists");
        }
        Game game = new Game();
        apply(game, request);
        Game saved = gameRepository.save(game);
        auditRecorder.record(AuditAction.CREATE_GAME, "Game", saved.getId(), null, mapper.toGame(saved, java.util.List.of(), java.util.List.of()));
        return mapper.toGame(saved, java.util.List.of(), java.util.List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<GameResponse> listGames(String keyword, GameStatus status, int page, int size) {
        Specification<Game> spec = (root, query, cb) -> cb.isFalse(root.get("deleted"));
        if (StringUtils.hasText(keyword)) {
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("slug")), "%" + keyword.toLowerCase() + "%")));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        return PageResponse.from(gameRepository.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("name")))
                .map(game -> mapper.toGame(game, rankRepository.findByGame_Id(game.getId()), roleRepository.findByGame_Id(game.getId()))));
    }

    @Transactional
    public GameResponse updateGame(UUID id, GameRequest request) {
        Game game = game(id);
        GameResponse before = mapper.toGame(game, rankRepository.findByGame_Id(id), roleRepository.findByGame_Id(id));
        apply(game, request);
        GameResponse after = mapper.toGame(game, rankRepository.findByGame_Id(id), roleRepository.findByGame_Id(id));
        auditRecorder.record(AuditAction.UPDATE_GAME, "Game", id, before, after);
        return after;
    }

    @Transactional
    public void deleteGame(UUID id) {
        Game game = game(id);
        game.softDelete();
        game.setStatus(GameStatus.INACTIVE);
        auditRecorder.record(AuditAction.DELETE_GAME, "Game", id, null, "SOFT_DELETED");
    }

    @Transactional
    public GameRankResponse addRank(UUID gameId, GameRankRequest request) {
        Game game = game(gameId);
        if (rankRepository.existsByGame_IdAndCode(gameId, code(request.code()))) {
            throw new DuplicateResourceException("Game rank code already exists");
        }
        GameRank rank = new GameRank();
        rank.setGame(game);
        rank.setCode(code(request.code()));
        rank.setName(request.name());
        rank.setSortOrder(request.sortOrder());
        return mapper.toRank(rankRepository.save(rank));
    }

    @Transactional
    public GameRoleResponse addRole(UUID gameId, GameRoleRequest request) {
        Game game = game(gameId);
        if (roleRepository.existsByGame_IdAndCode(gameId, code(request.code()))) {
            throw new DuplicateResourceException("Game role code already exists");
        }
        GameRole role = new GameRole();
        role.setGame(game);
        role.setCode(code(request.code()));
        role.setName(request.name());
        role.setSortOrder(request.sortOrder());
        return mapper.toRole(roleRepository.save(role));
    }

    private void apply(Game game, GameRequest request) {
        game.setSlug(slug(request.slug()));
        game.setName(request.name());
        game.setDescription(request.description());
        game.setMaxLobbySize(request.maxLobbySize());
        game.setStatus(request.status() == null ? GameStatus.ACTIVE : request.status());
    }

    private Game game(UUID id) {
        return gameRepository.findById(id).filter(game -> !game.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Game not found"));
    }

    private String slug(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String code(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}

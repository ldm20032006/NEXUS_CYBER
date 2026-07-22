package demo.server.controller;

import demo.server.common.enums.GameStatus;
import demo.server.common.response.ApiResponse;
import demo.server.common.response.PageResponse;
import demo.server.dto.game.GameRankRequest;
import demo.server.dto.game.GameRankResponse;
import demo.server.dto.game.GameRequest;
import demo.server.dto.game.GameResponse;
import demo.server.dto.game.GameRoleRequest;
import demo.server.dto.game.GameRoleResponse;
import demo.server.service.game.GameAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/games")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
public class GameAdminController {

    private final GameAdminService gameAdminService;

    public GameAdminController(GameAdminService gameAdminService) {
        this.gameAdminService = gameAdminService;
    }

    @PostMapping
    public ApiResponse<GameResponse> createGame(@Valid @RequestBody GameRequest request) {
        return ApiResponse.ok(gameAdminService.createGame(request), "Game created");
    }

    @GetMapping
    public ApiResponse<PageResponse<GameResponse>> listGames(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) GameStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(gameAdminService.listGames(keyword, status, page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<GameResponse> updateGame(@PathVariable UUID id, @Valid @RequestBody GameRequest request) {
        return ApiResponse.ok(gameAdminService.updateGame(id, request), "Game updated");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGame(@PathVariable UUID id) {
        gameAdminService.deleteGame(id);
        return ApiResponse.ok(null, "Game deleted");
    }

    @PostMapping("/{id}/ranks")
    public ApiResponse<GameRankResponse> addRank(@PathVariable UUID id, @Valid @RequestBody GameRankRequest request) {
        return ApiResponse.ok(gameAdminService.addRank(id, request), "Game rank created");
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<GameRoleResponse> addRole(@PathVariable UUID id, @Valid @RequestBody GameRoleRequest request) {
        return ApiResponse.ok(gameAdminService.addRole(id, request), "Game role created");
    }
}

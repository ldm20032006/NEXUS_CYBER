package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.ordering.MenuCategoryResponse;
import demo.server.dto.ordering.MenuItemResponse;
import demo.server.service.ordering.MenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<MenuCategoryResponse>> categories(@RequestParam UUID branchId) {
        return ApiResponse.ok(menuService.categories(branchId));
    }

    @GetMapping("/items")
    public ApiResponse<List<MenuItemResponse>> items(@RequestParam UUID branchId) {
        return ApiResponse.ok(menuService.items(branchId));
    }

    @GetMapping("/items/{id}")
    public ApiResponse<MenuItemResponse> item(@PathVariable UUID id) {
        return ApiResponse.ok(menuService.item(id));
    }
}

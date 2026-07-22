package demo.server.controller;

import demo.server.common.response.ApiResponse;
import demo.server.dto.ordering.MenuCategoryRequest;
import demo.server.dto.ordering.MenuCategoryResponse;
import demo.server.dto.ordering.MenuItemRequest;
import demo.server.dto.ordering.MenuItemResponse;
import demo.server.service.ordering.MenuService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/menu")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','BRANCH_ADMIN')")
public class MenuAdminController {

    private final MenuService menuService;

    public MenuAdminController(MenuService menuService) {
        this.menuService = menuService;
    }

    @PostMapping("/categories")
    public ApiResponse<MenuCategoryResponse> createCategory(@Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok(menuService.createCategory(request), "Menu category created");
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<MenuCategoryResponse> updateCategory(@PathVariable UUID id,
                                                            @Valid @RequestBody MenuCategoryRequest request) {
        return ApiResponse.ok(menuService.updateCategory(id, request), "Menu category updated");
    }

    @PostMapping("/items")
    public ApiResponse<MenuItemResponse> createItem(@Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(menuService.createItem(request), "Menu item created");
    }

    @PutMapping("/items/{id}")
    public ApiResponse<MenuItemResponse> updateItem(@PathVariable UUID id,
                                                    @Valid @RequestBody MenuItemRequest request) {
        return ApiResponse.ok(menuService.updateItem(id, request), "Menu item updated");
    }
}

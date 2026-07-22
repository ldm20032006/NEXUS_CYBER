package demo.server.service.ordering;

import demo.server.common.enums.AuditAction;
import demo.server.common.enums.MenuItemStatus;
import demo.server.common.audit.AuditRecorder;
import demo.server.dto.ordering.MenuCategoryRequest;
import demo.server.dto.ordering.MenuCategoryResponse;
import demo.server.dto.ordering.MenuItemRequest;
import demo.server.dto.ordering.MenuItemResponse;
import demo.server.entity.branch.Branch;
import demo.server.entity.ordering.MenuCategory;
import demo.server.entity.ordering.MenuItem;
import demo.server.exception.BusinessRuleException;
import demo.server.exception.DuplicateResourceException;
import demo.server.exception.ResourceNotFoundException;
import demo.server.repository.branch.BranchRepository;
import demo.server.repository.ordering.MenuCategoryRepository;
import demo.server.repository.ordering.MenuItemRepository;
import demo.server.service.branch.BranchScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final BranchScope branchScope;
    private final OrderMapper mapper;
    private final AuditRecorder auditRecorder;

    public MenuService(MenuCategoryRepository categoryRepository, MenuItemRepository itemRepository,
                       BranchRepository branchRepository, BranchScope branchScope, OrderMapper mapper,
                       AuditRecorder auditRecorder) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.branchScope = branchScope;
        this.mapper = mapper;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> categories(UUID branchId) {
        return categoryRepository.findByBranch_IdAndDeletedFalseOrderBySortOrderAscNameAsc(branchId).stream()
                .filter(MenuCategory::getActive)
                .map(mapper::toCategory)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> items(UUID branchId) {
        return itemRepository.findByBranch_IdAndDeletedFalseOrderByNameAsc(branchId).stream()
                .filter(item -> item.getStatus() != MenuItemStatus.INACTIVE)
                .map(mapper::toMenuItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse item(UUID id) {
        return mapper.toMenuItem(itemEntity(id));
    }

    @Transactional
    public MenuCategoryResponse createCategory(MenuCategoryRequest request) {
        Branch branch = branch(request.branchId());
        branchScope.assertBranchAllowed(branch.getId());
        categoryRepository.findByBranch_IdAndCode(branch.getId(), request.code())
                .ifPresent(existing -> { throw new DuplicateResourceException("Menu category code already exists in branch"); });
        MenuCategory category = new MenuCategory();
        category.setBranch(branch);
        apply(category, request);
        MenuCategory saved = categoryRepository.save(category);
        auditRecorder.record(AuditAction.SYSTEM_CONFIGURATION_CHANGE, "MenuCategory", saved.getId(), null, mapper.toCategory(saved));
        return mapper.toCategory(saved);
    }

    @Transactional
    public MenuCategoryResponse updateCategory(UUID id, MenuCategoryRequest request) {
        MenuCategory category = category(id);
        branchScope.assertBranchAllowed(category.getBranch().getId());
        if (!category.getBranch().getId().equals(request.branchId())) {
            throw new BusinessRuleException("Menu category branch cannot be changed");
        }
        MenuCategoryResponse before = mapper.toCategory(category);
        apply(category, request);
        auditRecorder.record(AuditAction.SYSTEM_CONFIGURATION_CHANGE, "MenuCategory", category.getId(), before, mapper.toCategory(category));
        return mapper.toCategory(category);
    }

    @Transactional
    public MenuItemResponse createItem(MenuItemRequest request) {
        Branch branch = branch(request.branchId());
        branchScope.assertBranchAllowed(branch.getId());
        MenuCategory category = category(request.categoryId());
        if (!category.getBranch().getId().equals(branch.getId())) {
            throw new BusinessRuleException("Menu category must belong to item branch");
        }
        itemRepository.findByBranch_IdAndCode(branch.getId(), request.code())
                .ifPresent(existing -> { throw new DuplicateResourceException("Menu item code already exists in branch"); });
        MenuItem item = new MenuItem();
        item.setBranch(branch);
        item.setCategory(category);
        apply(item, request);
        MenuItem saved = itemRepository.save(item);
        auditRecorder.record(AuditAction.SYSTEM_CONFIGURATION_CHANGE, "MenuItem", saved.getId(), null, mapper.toMenuItem(saved));
        return mapper.toMenuItem(saved);
    }

    @Transactional
    public MenuItemResponse updateItem(UUID id, MenuItemRequest request) {
        MenuItem item = itemEntity(id);
        branchScope.assertBranchAllowed(item.getBranch().getId());
        if (!item.getBranch().getId().equals(request.branchId())) {
            throw new BusinessRuleException("Menu item branch cannot be changed");
        }
        MenuCategory category = category(request.categoryId());
        if (!category.getBranch().getId().equals(item.getBranch().getId())) {
            throw new BusinessRuleException("Menu category must belong to item branch");
        }
        MenuItemResponse before = mapper.toMenuItem(item);
        item.setCategory(category);
        apply(item, request);
        auditRecorder.record(AuditAction.SYSTEM_CONFIGURATION_CHANGE, "MenuItem", item.getId(), before, mapper.toMenuItem(item));
        return mapper.toMenuItem(item);
    }

    private void apply(MenuCategory category, MenuCategoryRequest request) {
        category.setCode(request.code());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.active() == null || request.active());
    }

    private void apply(MenuItem item, MenuItemRequest request) {
        item.setCode(request.code());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setImageUrl(request.imageUrl());
        item.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
        item.setStockQuantity(request.stockQuantity());
        item.setEstimatedPrepMinutes(request.estimatedPrepMinutes());
        item.setStatus(request.status() == null ? MenuItemStatus.ACTIVE : request.status());
    }

    private Branch branch(UUID id) {
        return branchRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    private MenuCategory category(UUID id) {
        return categoryRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found"));
    }

    private MenuItem itemEntity(UUID id) {
        return itemRepository.findById(id).filter(item -> !item.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }
}

package demo.server.repository.ordering;

import demo.server.entity.ordering.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {
    Optional<MenuCategory> findByCode(String code);

    Optional<MenuCategory> findByBranch_IdAndCode(UUID branchId, String code);

    List<MenuCategory> findByBranch_IdAndDeletedFalseOrderBySortOrderAscNameAsc(UUID branchId);
}

package demo.server.repository.ordering;

import demo.server.entity.ordering.MenuItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {
    List<MenuItem> findByCategoryId(UUID categoryId);

    List<MenuItem> findByBranch_IdAndDeletedFalseOrderByNameAsc(UUID branchId);

    Optional<MenuItem> findByBranch_IdAndCode(UUID branchId, String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from MenuItem i where i.id = :id")
    Optional<MenuItem> findByIdForUpdate(UUID id);
}

package demo.server.repository.ordering;

import demo.server.common.enums.OrderStatus;
import demo.server.entity.ordering.FoodOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FoodOrderRepository extends JpaRepository<FoodOrder, UUID> {
    List<FoodOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FoodOrder> findByBranchIdAndStatusOrderByCreatedAtAsc(UUID branchId, OrderStatus status);

    List<FoodOrder> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    Optional<FoodOrder> findByIdempotencyKey(String idempotencyKey);

    @Query("select o from FoodOrder o where o.id = :id and (o.user.id = :userId or o.branch.id = :branchId)")
    Optional<FoodOrder> findScoped(UUID id, UUID userId, UUID branchId);
}

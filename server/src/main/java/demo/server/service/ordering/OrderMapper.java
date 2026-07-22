package demo.server.service.ordering;

import demo.server.dto.ordering.FoodOrderResponse;
import demo.server.dto.ordering.MenuCategoryResponse;
import demo.server.dto.ordering.MenuItemResponse;
import demo.server.dto.ordering.OrderItemResponse;
import demo.server.entity.ordering.FoodOrder;
import demo.server.entity.ordering.MenuCategory;
import demo.server.entity.ordering.MenuItem;
import demo.server.entity.ordering.OrderItem;
import demo.server.repository.ordering.OrderItemRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;

    public OrderMapper(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public MenuCategoryResponse toCategory(MenuCategory category) {
        return new MenuCategoryResponse(category.getId(), category.getBranch().getId(), category.getCode(),
                category.getName(), category.getDescription(), category.getSortOrder(), category.getActive());
    }

    public MenuItemResponse toMenuItem(MenuItem item) {
        return new MenuItemResponse(item.getId(), item.getBranch().getId(), item.getCategory().getId(), item.getCode(),
                item.getName(), item.getDescription(), item.getImageUrl(), item.getPrice(), item.getStockQuantity(),
                item.getEstimatedPrepMinutes(), item.getStatus());
    }

    public FoodOrderResponse toOrder(FoodOrder order) {
        return new FoodOrderResponse(order.getId(), order.getUser().getId(), order.getBranch().getId(),
                order.getStation() == null ? null : order.getStation().getId(), order.getPlaySession().getId(),
                order.getStatus(), order.getPaymentMethod(), order.getTotalAmount(), order.getNote(),
                order.getCancelReason(), order.getCreatedAt(), order.getAcceptedAt(), order.getPreparingAt(),
                order.getReadyAt(), order.getDeliveredAt(), order.getCancelledAt(),
                orderItemRepository.findByOrderId(order.getId()).stream().map(this::toOrderItem).toList());
    }

    public OrderItemResponse toOrderItem(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getMenuItem().getId(), item.getItemNameSnapshot(),
                item.getUnitPrice(), item.getQuantity(), item.getLineTotal(), item.getNote());
    }
}

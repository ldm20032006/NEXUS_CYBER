package demo.server.entity.ordering;

import demo.server.common.entity.BaseEntity;
import demo.server.common.enums.MenuItemStatus;
import demo.server.entity.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "menu_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_menu_item_code", columnNames = {"branch_id", "code"})
)
public class MenuItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private MenuCategory category;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    private Integer stockQuantity;

    private Integer estimatedPrepMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MenuItemStatus status = MenuItemStatus.ACTIVE;
}

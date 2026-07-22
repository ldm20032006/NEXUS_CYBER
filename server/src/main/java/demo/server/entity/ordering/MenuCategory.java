package demo.server.entity.ordering;

import demo.server.common.entity.BaseEntity;
import demo.server.entity.branch.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "menu_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_menu_category_branch_code", columnNames = {"branch_id", "code"})
)
public class MenuCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    private Integer sortOrder;

    private Boolean active = Boolean.TRUE;
}

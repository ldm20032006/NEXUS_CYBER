package demo.server.entity.gamer;

import demo.server.common.entity.BaseEntity;
import demo.server.entity.auth.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "gamer_profiles")
public class GamerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(length = 120)
    private String nickname;

    @Column(length = 500)
    private String avatarUrl;

    private LocalDate dateOfBirth;

    private Integer heightCm;

    private Integer weightKg;

    private Boolean nightMode = Boolean.FALSE;

    @Column(length = 1000)
    private String bio;
}

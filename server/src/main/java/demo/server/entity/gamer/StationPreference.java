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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "station_preferences")
public class StationPreference extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    private Integer deskHeightCm;

    private Integer chairAngleDegree;

    @Column(length = 20)
    private String rgbColor;

    private Integer brightness;

    private Integer mouseDpi;

    private Boolean nightMode = Boolean.FALSE;
}

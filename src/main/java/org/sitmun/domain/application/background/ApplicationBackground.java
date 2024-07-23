package org.sitmun.domain.application.background;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.background.Background;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Relationship between applications and backgrounds.
 */
@Entity
@Table(name = "STM_APP_BCKG", uniqueConstraints = @UniqueConstraint(name = "STM_APF_UK", columnNames = {"ABC_APPID", "ABC_BACKID"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationBackground {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_APP_BCKG_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "ABC_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APP_BCKG_GEN")
  @Column(name = "ABC_ID")
  private Integer id;

  /**
   * Application.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "ABC_APPID", foreignKey = @ForeignKey(name = "STM_ABC_FK_APP"))
  @NotNull
  private Application application;

  /**
   * Background.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "ABC_BACKID", foreignKey = @ForeignKey(name = "STM_ABC_FK_FON"))
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Background background;

  /**
   * Order of preference.
   * It can be used for sorting the list of backgrounds in a view.
   */
  @Column(name = "ABC_ORDER", precision = 6)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer order;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof ApplicationBackground)) {
        return false;
    }

    ApplicationBackground other = (ApplicationBackground) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

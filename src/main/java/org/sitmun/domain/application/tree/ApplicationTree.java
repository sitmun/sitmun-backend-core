package org.sitmun.domain.application.tree;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.client.dto.ClientConfigurationViews;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.tree.Tree;

/** Relationship between applications and trees. */
@Entity
@Table(
    name = "STM_APP_TREE",
    uniqueConstraints =
        @UniqueConstraint(
            name = "STM_ATR_UK",
            columnNames = {"ATR_APPID", "ATR_TREEID"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApplicationTree {

  @TableGenerator(
      name = "STM_APP_TREE_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "ATR_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APP_TREE_GEN")
  @Column(name = "ATR_ID")
  private Integer id;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "ATR_APPID", foreignKey = @ForeignKey(name = "STM_ATR_FK_APP"))
  @NotNull
  private Application application;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "ATR_TREEID", foreignKey = @ForeignKey(name = "STM_ATR_FK_TRE"))
  @NotNull
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Tree tree;

  @Column(name = "ATR_ORDER", precision = 6)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  @Builder.Default
  private Integer order = 0;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof ApplicationTree other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

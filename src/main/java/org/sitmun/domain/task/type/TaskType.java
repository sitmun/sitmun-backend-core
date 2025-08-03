package org.sitmun.domain.task.type;

import static org.sitmun.domain.PersistenceConstants.IDENTIFIER;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.*;
import org.hibernate.Length;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.infrastructure.persistence.type.i18n.I18n;
import org.sitmun.infrastructure.persistence.type.map.HashMapConverter;

/** Task type. */
@Entity
@Table(name = "STM_TSK_TYP")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskType {

  /** Unique identifier. */
  @TableGenerator(
      name = "STM_TSK_TYP_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "TTY_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TSK_TYP_GEN")
  @Column(name = "TTY_ID")
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private Integer id;

  /** Task type name. */
  @Column(name = "TTY_NAME", length = IDENTIFIER)
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /** Task type title. */
  @Column(name = "TTY_TITLE", length = PersistenceConstants.TITLE)
  @I18n
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String title;

  /** Is active. */
  @Column(name = "TTY_ENABLED")
  private Boolean enabled;

  /** Task type parent. */
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "TTY_PARENTID", foreignKey = @ForeignKey(name = "STM_TSK_TYP_TTY"))
  @ManyToOne
  private TaskType parent;

  /** Order in the UI. */
  @Column(name = "TTY_ORDER")
  private Integer order;

  /** */
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<TaskType> children = new HashSet<>();

  /** Task type specification */
  @Column(name = "TTY_SPEC", length = Length.LONG32)
  @Convert(converter = HashMapConverter.class)
  private Map<String, Object> specification;

  public Boolean getFolder() {
    if (children != null) {
      return !children.isEmpty();
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof TaskType other)) {
      return false;
    }

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

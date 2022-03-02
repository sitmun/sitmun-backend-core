package org.sitmun.domain;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.common.config.PersistenceConstants;
import org.sitmun.common.types.i18n.I18n;
import org.sitmun.common.types.map.HashMapConverter;
import org.sitmun.common.views.Views;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.sitmun.common.config.PersistenceConstants.IDENTIFIER;

/**
 * Task type.
 */
@Entity
@Table(name = "STM_TSK_TYP")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TaskType {

  /**
   * Unique identifier.
   */
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
  @JsonView(Views.WorkspaceApplication.class)
  private Integer id;

  /**
   * Task type name.
   */
  @Column(name = "TTY_NAME", length = IDENTIFIER)
  @JsonView(Views.WorkspaceApplication.class)
  private String name;

  /**
   * Task type title.
   */
  @Column(name = "TTY_TITLE", length = PersistenceConstants.TITLE)
  @I18n
  @JsonView(Views.WorkspaceApplication.class)
  private String title;

  /**
   * Is active.
   */
  @Column(name = "TTY_ENABLED")
  private Boolean enabled;

  /**
   * Task type parent.
   */
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "TTY_PARENTID", foreignKey = @ForeignKey(name = "STM_TSK_TYP_TTY"))
  @ManyToOne
  private TaskType parent;

  /**
   * Order in the UI.
   */
  @Column(name = "TTY_ORDER")
  private Integer order;

  /**
   *
   */
  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<TaskType> children = new HashSet<>();

  /**
   * Task type specification
   */
  @Lob
  @Column(name = "TTY_SPEC")
  @Convert(converter = HashMapConverter.class)
  private Map<String, Object> specification;

  public Boolean getFolder() {
    if (children != null) return !children.isEmpty();
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof TaskType))
      return false;

    TaskType other = (TaskType) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

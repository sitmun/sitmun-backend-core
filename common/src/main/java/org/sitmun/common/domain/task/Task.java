package org.sitmun.common.domain.task;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.common.domain.cartography.Cartography;
import org.sitmun.common.domain.database.DatabaseConnection;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.service.Service;
import org.sitmun.common.domain.task.availability.TaskAvailability;
import org.sitmun.common.domain.task.group.TaskGroup;
import org.sitmun.common.domain.task.relation.TaskRelation;
import org.sitmun.common.domain.task.type.TaskType;
import org.sitmun.common.domain.task.ui.TaskUI;
import org.sitmun.common.types.map.HashMapConverter;
import org.sitmun.feature.client.config.Views;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Task.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_TASK")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Task {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_TASK_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "TAS_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TASK_GEN")
  @Column(name = "TAS_ID")
  @JsonView(Views.WorkspaceApplication.class)
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "TAS_NAME", length = 512)
  @NotBlank
  @JsonView(Views.WorkspaceApplication.class)
  private String name;

  /**
   * Created date.
   */
  @Column(name = "TAS_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Order of preference.
   * It can be used for sorting the list of backgrounds in a view.
   */
  @Column(name = "TAS_ORDER", precision = 6)
  @JsonView(Views.WorkspaceApplication.class)
  private Integer order;

  /**
   * Task properties.
   */
  @Lob
  @Column(name = "TAS_PARAMS")
  @Convert(converter = HashMapConverter.class)
  @JsonView(Views.WorkspaceApplication.class)
  private Map<String, Object> properties;

  /**
   * Associated cartography.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_GIID", foreignKey = @ForeignKey(name = "STM_TAS_FK_GEO"))
  @JsonView(Views.WorkspaceApplication.class)
  private Cartography cartography;

  /**
   * Associated service.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_SERID", foreignKey = @ForeignKey(name = "STM_TAS_FK_SER"))
  @JsonView(Views.WorkspaceApplication.class)
  private Service service;

  /**
   * Task group.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_GTASKID", foreignKey = @ForeignKey(name = "STM_TAS_FK_GTS"))
  @JsonView(Views.WorkspaceApplication.class)
  private TaskGroup group;

  /**
   * Task type.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_TTASKID", foreignKey = @ForeignKey(name = "STM_TAS_FK_TTY"))
  @JsonView(Views.WorkspaceApplication.class)
  private TaskType type;

  /**
   * Task UI.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_TUIID", foreignKey = @ForeignKey(name = "STM_TAS_FK_TUI"))
  @JsonView(Views.WorkspaceApplication.class)
  private TaskUI ui;

  /**
   * Associated connection.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_CONNID", foreignKey = @ForeignKey(name = "STM_TAS_FK_CON"))
  private DatabaseConnection connection;

  /**
   * Roles allowed to access to this task.
   */
  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(
    name = "STM_ROL_TSK",
    joinColumns = @JoinColumn(
      name = "RTS_TASKID",
      foreignKey = @ForeignKey(name = "STM_RTS_FK_TAS")),
    inverseJoinColumns = @JoinColumn(
      name = "RTS_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RTS_FK_ROL")))
  @Builder.Default
  private Set<Role> roles = new HashSet<>();

  /**
   * Territorial availability of this task.
   */
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<TaskAvailability> availabilities = new HashSet<>();

  /**
   * Relations of where this task is the subject of the relation.
   */
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<TaskRelation> relations = new HashSet<>();

  /**
   * Relations of where this task is the target of the relation.
   */
  @OneToMany(mappedBy = "relatedTask")
  @Builder.Default
  private Set<TaskRelation> relatedBy = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Task))
      return false;

    Task other = (Task) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

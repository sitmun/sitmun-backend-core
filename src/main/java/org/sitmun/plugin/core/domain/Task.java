package org.sitmun.plugin.core.domain;


import lombok.*;
import org.sitmun.plugin.core.converters.HashMapConverter;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Task.
 */
@Entity
@Table(name = "STM_TASK")
@Inheritance(strategy = InheritanceType.JOINED)
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
  private Integer id;

  /**
   * Parent task.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_PARENTID")
  private Task parent;

  /**
   * Children tasks.
   */
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
  private Set<Task> children;

  /**
   * Name.
   */
  @Column(name = "TAS_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Created date.
   */
  @Column(name = "TAS_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Order of preference.
   * It can be used for sorting the list of backgrounds in a view.
   */
  @Column(name = "TAS_ORDER", precision = 6)
  private Integer order;

  @Lob
  @Column(name = "TAS_PARAMS")
  @Convert(converter = HashMapConverter.class)
  private Map<String, Object> properties;

  /**
   * Associated cartography.
   */
  @ManyToOne
  @JoinColumn(name = "TAS_PARENTID", insertable = false, updatable = false)
  private Cartography cartography;

  /**
   * Associated service.
   */
  @ManyToOne
  @JoinColumn(name = "TAS_SERID")
  private Service service;

  /**
   * Task group.
   */
  @ManyToOne
  @JoinColumn(name = "TAS_GTASKID", foreignKey = @ForeignKey(name = "STM_TAR_FK_GTA"))
  private TaskGroup group;

  /**
   * Task type.
   */
  @ManyToOne
  @JoinColumn(name = "TAS_TTASKID", foreignKey = @ForeignKey(name = "STM_TAR_FK_TTA"))
  private TaskType type;

  /**
   * Task UI.
   */
  @ManyToOne
  @JoinColumn(name = "TAS_TUIID", foreignKey = @ForeignKey(name = "STM_TAR_FK_TUI"))
  private TaskUI ui;

  /**
   * Associated connection.
   */
  @ManyToOne
  @JoinColumn(name = "TAS_CONNID", foreignKey = @ForeignKey(name = "STM_TAR_FK_CON"))
  private DatabaseConnection connection;

  /**
   * Roles allowed to access to this task.
   */
  @ManyToMany
  @JoinTable(
    name = "STM_ROL_TSK",
    joinColumns = @JoinColumn(
      name = "RTS_TASKID",
      foreignKey = @ForeignKey(name = "STM_RTA_FK_T")),
    inverseJoinColumns = @JoinColumn(
      name = "RTS_ROLEID",
      foreignKey = @ForeignKey(name = "STM_RTA_FK_ROL")))
  private Set<Role> roles;

  /**
   * Territorial availability of this task.
   */
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<TaskAvailability> availabilities = new HashSet<>();

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

package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;
//import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
//import org.springframework.hateoas.Identifiable;
//import org.springframework.hateoas.Link;
//import org.springframework.hateoas.Resource;
//import org.springframework.hateoas.ResourceSupport;

/**
 * Task.
 */
@Entity
@Table(name = "STM_TASK")
@Inheritance(strategy = InheritanceType.JOINED)
public class Task { //implements Identifiable {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_TAREA_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "TAS_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TAREA_GEN")
  @Column(name = "TAS_ID", precision = 11)
  private BigInteger id;

  /**
   * Parent task.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "TAS_PARENTID")
  @JsonIgnore
  private Task parent;

  /**
   * Children tasks.
   */
  @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
  @JsonIgnore
  private Set<Task> children = new HashSet<>();

  /**
   * Name.
   */
  @Column(name = "TAS_NAME", length = 250)
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
  private BigInteger order;

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
  private Cartography service;

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
  private Connection connection;

  /**
   * Roles allowed to access to this task.
   */
  @ManyToMany
  @JoinTable(
      name = "STM_ROL_TSK",
      joinColumns = @JoinColumn(
          name = "RTS_ROLEID",
          foreignKey = @ForeignKey(name = "STM_RTA_FK_ROL")),
      inverseJoinColumns = @JoinColumn(
          name = "RTS_TASKID",
          foreignKey = @ForeignKey(name = "STM_RTA_FK_T")))
  private Set<Role> roles;

  /**
   * Territorial availability of this task.
   */
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<TaskAvailability> availabilities = new HashSet<>();

  /**
   * Task parameters.
   */
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<TaskParameter> parameters = new HashSet<>();

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public Task getParent() {
    return parent;
  }

  public void setParent(Task parent) {
    this.parent = parent;
  }

  public Set<Task> getChildren() {
    return children;
  }

  public void setChildren(Set<Task> children) {
    this.children = children;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }

  public Cartography getService() {
    return service;
  }

  public void setService(Cartography service) {
    this.service = service;
  }

  public TaskGroup getGroup() {
    return group;
  }

  public void setGroup(TaskGroup group) {
    this.group = group;
  }

  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }

  public TaskUI getUi() {
    return ui;
  }

  public void setUi(TaskUI ui) {
    this.ui = ui;
  }

  public Connection getConnection() {
    return connection;
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public Set<Role> getRoles() {
    return roles;
  }

  public void setRoles(Set<Role> roles) {
    this.roles = roles;
  }

  public Set<TaskAvailability> getAvailabilities() {
    return availabilities;
  }

  public void setAvailabilities(
      Set<TaskAvailability> availabilities) {
    this.availabilities = availabilities;
  }

  public Set<TaskParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<TaskParameter> parameters) {
    this.parameters = parameters;
  }

  //  public ResourceSupport toResource(RepositoryEntityLinks links) {
  //    Link selfLink = links.linkForSingleResource(this).withSelfRel();
  //    ResourceSupport res = new Resource<>(this, selfLink);
  //    res.add(links.linkForSingleResource(this).slash("availabilities")
  //      .withRel("availabilities"));
  //    res.add(links.linkForSingleResource(this).slash("connection").withRel("connection"));
  //    res.add(links.linkForSingleResource(this).slash("group").withRel("group"));
  //    res.add(links.linkForSingleResource(this).slash("parameters").withRel("parameters"));
  //    res.add(links.linkForSingleResource(this).slash("roles").withRel("roles"));
  //    res.add(links.linkForSingleResource(this).slash("type").withRel("type"));
  //    res.add(links.linkForSingleResource(this).slash("ui").withRel("ui"));
  //    return res;
  //  }

}

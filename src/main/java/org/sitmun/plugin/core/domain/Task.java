package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "STM_TAREA")
public class Task implements Identifiable {

  @TableGenerator(
      name = "STM_TAREA_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "TAR_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_TAREA_GEN")
  @Column(name = "TAR_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "TAR_NOMBRE", length = 250)
  private String name;

  @Column(name = "TAR_ORDEN", precision = 6)
  private BigInteger order;

  @Column(name = "TAR_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @ManyToOne
  @JoinColumn(name = "TAR_CODCON", foreignKey = @ForeignKey(name = "STM_TAR_FK_CON"))
  private Connection connection;
  @ManyToMany
  @JoinTable(name = "STM_ROLTAR", joinColumns = @JoinColumn(name = "RTA_CODROL", foreignKey = @ForeignKey(name = "STM_RTA_FK_ROL")), inverseJoinColumns = @JoinColumn(name = "RTA_CODTAR", foreignKey = @ForeignKey(name = "STM_RTA_FK_T")))
  private Set<Role> roles;
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<TaskAvailability> availabilities = new HashSet<>();
  @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<TaskParameter> parameters = new HashSet<>();
  @ManyToOne
  @JoinColumn(name = "TAR_CODGTA", foreignKey = @ForeignKey(name = "STM_TAR_FK_GTA"))
  private TaskGroup group;
  @ManyToOne
  @JoinColumn(name = "TAR_CODTTA", foreignKey = @ForeignKey(name = "STM_TAR_FK_TTA"))
  private TaskType type;
  @ManyToOne
  @JoinColumn(name = "TAR_CODTUI", foreignKey = @ForeignKey(name = "STM_TAR_FK_TUI"))
  private TaskUI ui;

  public Set<TaskParameter> getParameters() {
    return parameters;
  }

  public void setParameters(Set<TaskParameter> parameters) {
    this.parameters = parameters;
  }

  public TaskUI getUi() {
    return ui;
  }

  public void setUi(TaskUI ui) {
    this.ui = ui;
  }

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
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

  public void setAvailabilities(Set<TaskAvailability> availabilities) {
    this.availabilities = availabilities;
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

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("availabilities").withRel("availabilities"));
    res.add(links.linkForSingleResource(this).slash("connection").withRel("connection"));
    res.add(links.linkForSingleResource(this).slash("group").withRel("group"));
    res.add(links.linkForSingleResource(this).slash("parameters").withRel("parameters"));
    res.add(links.linkForSingleResource(this).slash("roles").withRel("roles"));
    res.add(links.linkForSingleResource(this).slash("type").withRel("type"));
    res.add(links.linkForSingleResource(this).slash("ui").withRel("ui"));
    return res;
  }

}

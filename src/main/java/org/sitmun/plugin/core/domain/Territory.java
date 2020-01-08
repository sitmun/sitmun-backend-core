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
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "STM_ETERRIT", uniqueConstraints = {@UniqueConstraint(name = "STM_TER_NOM_UK", columnNames = {"TER_NOMBRE"})})
public class Territory {

  @TableGenerator(
      name = "STM_ETERRIT_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "TER_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ETERRIT_GEN")
  @Column(name = "TER_CODIGO", precision = 11)
  private BigInteger id;

  @NotNull
  @Column(name = "TER_NOMBRE", nullable = false, length = 250)
  private String name;

  @Column(name = "TER_CORREO", length = 250)
  private String email;

  @Column(name = "TER_DIRECC", length = 250)
  private String address;

  @Column(name = "TER_NADMIN", length = 250)
  private String organizationName;

  @Column(name = "TER_AMBITO", length = 250)
  private String scope;

  @Column(name = "TER_LOGO", length = 250)
  private String logo;

  @Column(name = "TER_EXT", length = 250)
  private String ext;

  @Column(name = "TER_BLOQ")
  private Boolean blocked;

  @Column(name = "TER_OBSERV", length = 250)
  private String comments;

  @Column(name = "TER_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(name = "STM_GRPTER", joinColumns = @JoinColumn(name = "GRT_CODTER", foreignKey = @ForeignKey(name = "STM_GRT_FK_TER")), inverseJoinColumns = @JoinColumn(name = "GRT_CODTERM", foreignKey = @ForeignKey(name = "STM_GRT_FK_TEM")))
  private Set<Territory> members = new HashSet<>();

  @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
  @JoinTable(name = "STM_GRPTER", joinColumns = @JoinColumn(name = "GRT_CODTERM", foreignKey = @ForeignKey(name = "STM_GRT_FK_TERM")), inverseJoinColumns = @JoinColumn(name = "GRT_CODTER", foreignKey = @ForeignKey(name = "STM_GRT_FK_TER")))
  private Set<Territory> memberOf = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "TER_CODTGR", foreignKey = @ForeignKey(name = "STM_TER_FK_TGR"))
  private TerritoryType type;

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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getLogo() {
    return logo;
  }

  public void setLogo(String logo) {
    this.logo = logo;
  }

  public String getExt() {
    return ext;
  }

  public void setExt(String ext) {
    this.ext = ext;
  }

  public Boolean getBlocked() {
    return blocked;
  }

  public void setBlocked(Boolean blocked) {
    this.blocked = blocked;
  }

  public String getComments() {
    return comments;
  }

  public void setComments(String comments) {
    this.comments = comments;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public Set<Territory> getMembers() {
    return members;
  }

  public void setMembers(Set<Territory> members) {
    this.members = members;
  }

  public Set<Territory> getMemberOf() {
    return memberOf;
  }

  public void setMemberOf(Set<Territory> memberOf) {
    this.memberOf = memberOf;
  }

  public TerritoryType getType() {
    return type;
  }

  public void setType(TerritoryType type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if ((o != null) && (o instanceof Territory)) {
      return ((Territory) o).getId() == this.getId();
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}

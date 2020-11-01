package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;
// import org.springframework.hateoas.Identifiable;

/**
 * User.
 */
@Entity
@Table(name = "STM_USER", uniqueConstraints = {
    @UniqueConstraint(name = "STM_USU_USU_UK", columnNames = {"USE_USER"})})
public class User { //implements Identifiable<BigInteger> {

  @TableGenerator(
      name = "STM_USER_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "USE_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_USER_GEN")
  @Column(name = "USE_ID", precision = 11)
  private BigInteger id;

  /**
   * User login.
   */
  @Column(name = "USE_USER", length = 30)
  private String username;

  /**
   * User password hash.
   */
  @Column(name = "USE_PWD", length = 128)
  private String password;

  /**
   * User first name.
   */
  @Column(name = "USE_NAME", length = 30)
  private String firstName;

  /**
   * User last name.
   */
  @Column(name = "USE_SURNAME", length = 40)
  private String lastName;

  /**
   * User identification number.
   */
  @Column(name = "USE_IDENT", length = 20)
  private String identificationNumber;

  /**
   * User identification type.
   */
  @Column(name = "USE_IDENTTYPE", length = 3)
  @CodeList(CodeLists.USER_IDENTIFICATION_TYPE)
  private String identificationType;

  /**
   * If <code>true</code>, the user is a system administrator.
   */
  @Column(name = "USE_ADM")
  @NotNull
  private Boolean administrator;

  /**
   * If <code>true</code>, the user is blocked and cannot log to the system.
   */
  @Column(name = "USE_BLOCKED")
  @NotNull
  private Boolean blocked;

  /**
   * If <code>true</code>, the user act on behalf of any citizen.
   */
  @Column(name = "USE_GENERIC")
  private Boolean generic;

  /**
   * User positions.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<UserPosition> positions = new HashSet<>();

  /**
   * User permissions.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private Set<UserConfiguration> permissions = new HashSet<>();

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getIdentificationNumber() {
    return identificationNumber;
  }

  public void setIdentificationNumber(String identificationNumber) {
    this.identificationNumber = identificationNumber;
  }

  public String getIdentificationType() {
    return identificationType;
  }

  public void setIdentificationType(String identificationType) {
    this.identificationType = identificationType;
  }

  public Boolean getAdministrator() {
    return administrator;
  }

  public void setAdministrator(Boolean administrator) {
    this.administrator = administrator;
  }

  public Boolean getBlocked() {
    return blocked;
  }

  public void setBlocked(Boolean blocked) {
    this.blocked = blocked;
  }

  public Boolean getGeneric() {
    return generic;
  }

  public void setGeneric(Boolean generic) {
    this.generic = generic;
  }

  public Set<UserPosition> getPositions() {
    return positions;
  }

  public void setPositions(Set<UserPosition> positions) {
    this.positions = positions;
  }

  public Set<UserConfiguration> getPermissions() {
    return permissions;
  }

  public void setPermissions(Set<UserConfiguration> permissions) {
    this.permissions = permissions;
  }
}

package org.sitmun.common.domain.user;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.sitmun.common.config.CodeLists;
import org.sitmun.common.config.PersistenceConstants;
import org.sitmun.common.domain.user.configuration.UserConfiguration;
import org.sitmun.common.domain.user.position.UserPosition;
import org.sitmun.common.types.codelist.CodeList;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * User.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_USER", uniqueConstraints = {
  @UniqueConstraint(name = "STM_USU_USU_UK", columnNames = {"USE_USER"})})
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

  @TableGenerator(
    name = "STM_USER_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "USE_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_USER_GEN")
  @Column(name = "USE_ID")
  private Integer id;

  /**
   * User login.
   */
  @Column(name = "USE_USER", length = PersistenceConstants.IDENTIFIER)
  private String username;

  /**
   * User password hash.
   */
  @Column(name = "USE_PWD", length = 128)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  @JsonIgnore
  @Transient
  private String storedPassword;

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
  @Column(name = "USE_IDENT", length = PersistenceConstants.IDENTIFIER)
  private String identificationNumber;

  /**
   * User identification type.
   */
  @Column(name = "USE_IDENTTYPE", length = PersistenceConstants.IDENTIFIER)
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
   * If <code>true</code>, the user acts on behalf of any citizen.
   */
  @Column(name = "USE_GENERIC")
  private Boolean generic;


  /**
   * Creation date.
   */
  @Column(name = "USE_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * User positions.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<UserPosition> positions = new HashSet<>();

  /**
   * User permissions.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<UserConfiguration> permissions = new HashSet<>();

  @PostLoad
  public void postLoad() {
    storedPassword = password;
  }

  /**
   * True if the password is set.
   *
   * @return true if password is not empty
   */
  public Boolean getPasswordSet() {
    return password != null && !password.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof User))
      return false;

    User other = (User) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

}

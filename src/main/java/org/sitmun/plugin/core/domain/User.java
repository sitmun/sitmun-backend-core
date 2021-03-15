package org.sitmun.plugin.core.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;
// import org.springframework.hateoas.Identifiable;

/**
 * User.
 */
@Entity
@Table(name = "STM_USER", uniqueConstraints = {
  @UniqueConstraint(name = "STM_USU_USU_UK", columnNames = {"USE_USER"})})
@Builder
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
  @Column(name = "USE_USER", length = IDENTIFIER)
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
  @Column(name = "USE_IDENT", length = IDENTIFIER)
  private String identificationNumber;

  /**
   * User identification type.
   */
  @Column(name = "USE_IDENTTYPE", length = IDENTIFIER)
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
   * User positions.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<UserPosition> positions = new HashSet<>();

  /**
   * User permissions.
   */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private Set<UserConfiguration> permissions = new HashSet<>();

  @PostLoad
  public void postLoad() {
    storedPassword = password;
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

package org.sitmun.domain.database;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.task.Task;
import org.sitmun.infrastructure.persistence.type.codelist.CodeList;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a JDBC database connection.
 */
@Entity
@Table(name = "STM_CONNECT")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DatabaseConnection {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_CONNECT_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "CON_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CONNECT_GEN")
  @Column(name = "CON_ID")
  private Integer id;

  /**
   * Connection name.
   */
  @Column(name = "CON_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * JDBC driver.
   */
  @Column(name = "CON_DRIVER", length = PersistenceConstants.IDENTIFIER)
  @CodeList(CodeListsConstants.DATABASE_CONNECTION_DRIVER)
  private String driver;

  /**
   * User.
   */
  @Column(name = "CON_USER", length = PersistenceConstants.IDENTIFIER)
  private String user;

  /**
   * Password.
   */
  @Column(name = "CON_PWD", length = 50)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private String password;

  @JsonIgnore
  @Transient
  private String storedPassword;

  /**
   * JDBC connection string.
   */
  @Column(name = "CON_CONNECTION", length = 250)
  private String url;


  /**
   * Tasks that use this connection.
   */
  @OneToMany(mappedBy = "connection")
  @Builder.Default
  private Set<Task> tasks = new HashSet<>();

  /**
   * Cartographies that use this connection.
   */
  @OneToMany(mappedBy = "spatialSelectionConnection")
  @Builder.Default
  private Set<Cartography> cartographies = new HashSet<>();

  /**
   * True if the password is set.
   *
   * @return true if password is not empty
   */
  public Boolean getPasswordSet() {
    return password != null && !password.isEmpty();
  }

  @PostLoad
  public void postLoad() {
    storedPassword = password;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof DatabaseConnection)) {
        return false;
    }

    DatabaseConnection other = (DatabaseConnection) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

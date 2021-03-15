package org.sitmun.plugin.core.domain;


import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

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
  @Column(name = "CON_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * JDBC driver.
   */
  @Column(name = "CON_DRIVER", length = IDENTIFIER)
  @NotBlank
  private String driver;

  /**
   * User.
   */
  @Column(name = "CON_USER", length = IDENTIFIER)
  private String user;

  /**
   * Password.
   */
  @Column(name = "CON_PWD", length = 50)
  private String password;

  /**
   * JDBC connection string.
   */
  @Column(name = "CON_CONNECTION", length = 250)
  private String url;


  /**
   * Tasks that use this connection.
   */
  @OneToMany(mappedBy = "connection", cascade = CascadeType.ALL)
  private Set<Task> tasks;

  /**
   * Cartographies that use this connection.
   */
  @OneToMany(mappedBy = "spatialSelectionConnection", cascade = CascadeType.ALL)
  private Set<Cartography> cartographies;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DatabaseConnection))
      return false;

    DatabaseConnection other = (DatabaseConnection) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

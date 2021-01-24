package org.sitmun.plugin.core.domain;


import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Represents a JDBC database connection.
 */
@Entity
@Table(name = "STM_CONNECT")
public class Connection {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_CONEXION_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "CON_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CONEXION_GEN")
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
  private Set<Task> tasks = new HashSet<>();

  /**
   * Cartographies that use this connection.
   */
  @OneToMany(mappedBy = "spatialSelectionConnection", cascade = CascadeType.ALL)
  private Set<Cartography> cartographies = new HashSet<>();


  public Connection() {
  }

  private Connection(Integer id, @NotBlank String name,
                     @NotBlank String driver, String user, String password,
                     String url, Set<Task> tasks, Set<Cartography> cartographies) {
    this.id = id;
    this.name = name;
    this.driver = driver;
    this.user = user;
    this.password = password;
    this.url = url;
    this.tasks = tasks;
    this.cartographies = cartographies;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDriver() {
    return driver;
  }

  public void setDriver(String driver) {
    this.driver = driver;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Set<Task> getTasks() {
    return tasks;
  }

  public void setTasks(Set<Task> tasks) {
    this.tasks = tasks;
  }

  public Set<Cartography> getCartographies() {
    return cartographies;
  }

  public void setCartographies(Set<Cartography> cartographies) {
    this.cartographies = cartographies;
  }

  public static class Builder {
    private Integer id;
    private @NotBlank String name;
    private @NotBlank String driver;
    private String user;
    private String password;
    private String url;
    private Set<Task> tasks;
    private Set<Cartography> cartographies;

    public Builder setId(Integer id) {
      this.id = id;
      return this;
    }

    public Builder setName(@NotBlank String name) {
      this.name = name;
      return this;
    }

    public Builder setDriver(@NotBlank String driver) {
      this.driver = driver;
      return this;
    }

    public Builder setUser(String user) {
      this.user = user;
      return this;
    }

    public Builder setPassword(String password) {
      this.password = password;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setTaks(Set<Task> tasks) {
      this.tasks = tasks;
      return this;
    }

    public Builder setCartographies(Set<Cartography> cartographies) {
      this.cartographies = cartographies;
      return this;
    }

    public Connection build() {
      return new Connection(id, name, driver, user, password, url, tasks, cartographies);
    }
  }
}

package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.NotBlank;

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
  @Column(name = "CON_ID", precision = 11)
  private BigInteger id;

  /**
   * Connection name.
   */
  @Column(name = "CON_NAME", length = 80)
  @NotBlank
  private String name;

  /**
   * JDBC driver.
   */
  @Column(name = "CON_DRIVER", length = 50)
  @NotBlank
  private String driver;

  /**
   * User.
   */
  @Column(name = "CON_USER", length = 50)
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

  public Connection() {
  }

  private Connection(BigInteger id, @NotBlank String name,
                     @NotBlank String driver, String user, String password,
                     String url) {
    this.id = id;
    this.name = name;
    this.driver = driver;
    this.user = user;
    this.password = password;
    this.url = url;
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

  public static Builder builder() {
    return new Builder();
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

  public static class Builder {
    private BigInteger id;
    private @NotBlank String name;
    private @NotBlank String driver;
    private String user;
    private String password;
    private String url;

    public Builder setId(BigInteger id) {
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

    public Connection build() {
      return new Connection(id, name, driver, user, password, url);
    }
  }
}

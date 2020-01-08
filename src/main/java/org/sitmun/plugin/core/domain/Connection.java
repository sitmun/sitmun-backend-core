package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "STM_CONEXION")
public class Connection {

  @TableGenerator(
      name = "STM_CONEXION_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "CON_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_CONEXION_GEN")
  @Column(name = "CON_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "CON_NOMBRE", length = 80)
  private String name;

  @Column(name = "CON_DRIVER", length = 50)
  private String type;

  @Column(name = "CON_USUARIO", length = 50)
  private String user;

  @Column(name = "CON_PASSWORD", length = 50)
  private String password;

  @Column(name = "CON_CONSTRING", length = 250)
  private String connectionString;

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

}

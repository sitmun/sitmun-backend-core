package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Application parameter.
 */
@Entity
@Table(name = "STM_PAR_APP")
public class ApplicationParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_PARAMAPP_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "PAP_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PARAMAPP_GEN")
  @Column(name = "PAP_ID", precision = 11)
  private BigInteger id;

  /**
   * Application parameter name.
   */
  @Column(name = "PAP_NAME", length = 30)
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PAP_VALUE", length = 250)
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PAP_TYPE", length = 250)
  private String type;

  /**
   * Application that applies this parameter.
   */
  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PAP_APPID", foreignKey = @ForeignKey(name = "STM_PAP_FK_APP"))
  private Application application;

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

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

}

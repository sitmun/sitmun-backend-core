package org.sitmun.plugin.core.domain;


import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;


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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

/**
 * Service parameter.
 */
@Entity
@Table(name = "STM_PAR_SER")
public class ServiceParameter {

  /**
   * Unique identifier.
   */
  @TableGenerator(
      name = "STM_PARAMSER_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "PSE_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PARAMSER_GEN")
  @Column(name = "PSE_ID")
  private Integer id;

  /**
   * Parameter name.
   */
  @Column(name = "PSE_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Parameter value.
   */
  @Column(name = "PSE_VALUE", length = 250)
  private String value;

  /**
   * Parameter type.
   */
  @Column(name = "PSE_TYPE", length = IDENTIFIER)
  @CodeList(CodeLists.SERVICE_PARAMETER_TYPE)
  @NotNull
  private String type;

  /**
   * Service that applies this parameter.
   */
  @ManyToOne
  @NotNull
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PSE_SERID", foreignKey = @ForeignKey(name = "STM_PSE_FK_SER"))
  private Service service;

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

  public Service getService() {
    return service;
  }

  public void setService(Service service) {
    this.service = service;
  }

}

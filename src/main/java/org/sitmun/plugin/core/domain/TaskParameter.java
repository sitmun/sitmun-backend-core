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

@Entity
@Table(name = "STM_PARAMTTA")
public class TaskParameter {

  @TableGenerator(
      name = "STM_PARAMTTA_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "PTT_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_PARAMTTA_GEN")
  @Column(name = "PTT_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "PTT_NOMBRE", length = 50)
  private String name;

  @Column(name = "PTT_VALOR", length = 512)
  private String value;

  @Column(name = "PTT_TIPO", length = 30)
  private String type;

  @ManyToOne
  @NotNull
  // @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "PTT_CODTAR", foreignKey = @ForeignKey(name = "STM_PTT_FK_TAR"))
  private Task task;

  @Column(name = "PTT_ORDEN", precision = 6)
  private BigInteger order;

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

  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

}

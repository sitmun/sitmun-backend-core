package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

@Entity
@Table(name = "STM_ARBOLNOD")
public class TreeNode {

  @TableGenerator(
      name = "STM_ARBOLNOD_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "ANR_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_ARBOLNOD_GEN")
  @Column(name = "ANR_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "ARN_NOMBRE", length = 80)
  private String name;

  @Column(name = "ARN_TOOLTIP", length = 100)
  private String tooltip;

  @Column(name = "ARN_ORDEN", precision = 6)
  private BigInteger orden;

  @Column(name = "ARN_ACTIVO")
  private Boolean active;

  @JoinColumn(name = "ARN_CODPADRE", foreignKey = @ForeignKey(name = "STM_ARN_FK_ARN"))
  @ManyToOne
  @JsonIgnore
  private TreeNode parent;

  @JoinColumn(name = "ARN_CODARB", foreignKey = @ForeignKey(name = "STM_ARN_FK_ARB"))
  @ManyToOne
  private Tree tree;

  @JoinColumn(name = "ARN_CODCAR", foreignKey = @ForeignKey(name = "STM_ARN_FK_CAR"))
  @ManyToOne
  private Cartography cartography;

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

  public String getTooltip() {
    return tooltip;
  }

  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  public BigInteger getOrden() {
    return orden;
  }

  public void setOrden(BigInteger orden) {
    this.orden = orden;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public TreeNode getParent() {
    return parent;
  }

  public void setParent(TreeNode parent) {
    this.parent = parent;
  }

  public Tree getTree() {
    return tree;
  }

  public void setTree(Tree tree) {
    this.tree = tree;
  }

  public Cartography getCartography() {
    return cartography;
  }

  public void setCartography(Cartography cartography) {
    this.cartography = cartography;
  }

}

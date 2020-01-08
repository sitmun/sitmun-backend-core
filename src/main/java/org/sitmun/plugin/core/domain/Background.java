package org.sitmun.plugin.core.domain;

import java.math.BigInteger;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "STM_FONDO")
public class Background implements Identifiable {

  @TableGenerator(
      name = "STM_FONDO_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "FON_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_FONDO_GEN")
  @Column(name = "FON_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "FON_NOMBRE", length = 30)
  private String name;

  @Column(name = "FON_DESC", length = 250)
  private String description;

  @Column(name = "FON_ACTIVO")
  private Boolean active;

  @Column(name = "FON_F_ALTA")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  @ManyToOne
  @JoinColumn(name = "FON_CODGCA", foreignKey = @ForeignKey(name = "STM_FON_FK_GCA"))
  @NotNull
  private CartographyGroup cartographyGroup;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  public CartographyGroup getCartographyGroup() {
    return cartographyGroup;
  }

  public void setCartographyGroup(CartographyGroup cartographyGroup) {
    this.cartographyGroup = cartographyGroup;
  }

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("cartographyGroup").withRel("cartographyGroup"));
    return res;
  }

}

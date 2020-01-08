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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Identifiable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;

@Entity
@Table(name = "STM_APPFON", uniqueConstraints = {@UniqueConstraint(name = "STM_APF_UK", columnNames = {"APF_CODAPP", "APF_CODFON"})})
public class ApplicationBackground implements Identifiable {


  @TableGenerator(
      name = "STM_APPFON_GEN",
      table = "STM_CODIGOS",
      pkColumnName = "GEN_CODIGO",
      valueColumnName = "GEN_VALOR",
      pkColumnValue = "APF_CODIGO",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_APPFON_GEN")
  @Column(name = "APF_CODIGO", precision = 11)
  private BigInteger id;

  @Column(name = "APF_ORDEN", precision = 6)
  private BigInteger order;

  @ManyToOne
  @JoinColumn(name = "APF_CODAPP", foreignKey = @ForeignKey(name = "STM_APF_FK_APP"))
  // @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Application application;

  @ManyToOne
  @JoinColumn(name = "APF_CODFON", foreignKey = @ForeignKey(name = "STM_APF_FK_FON"))
  // @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Background background;

  public BigInteger getId() {
    return id;
  }

  public void setId(BigInteger id) {
    this.id = id;
  }

  public BigInteger getOrder() {
    return order;
  }

  public void setOrder(BigInteger order) {
    this.order = order;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  public Background getBackground() {
    return background;
  }

  public void setBackground(Background background) {
    this.background = background;
  }

  public ResourceSupport toResource(RepositoryEntityLinks links) {
    Link selfLink = links.linkForSingleResource(this).withSelfRel();
    ResourceSupport res = new Resource<>(this, selfLink);
    res.add(links.linkForSingleResource(this).slash("application").withRel("application"));
    res.add(links.linkForSingleResource(this).slash("background").withRel("background"));
    return res;
  }

}

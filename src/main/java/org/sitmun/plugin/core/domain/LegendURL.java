package org.sitmun.plugin.core.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

/**
 * Legend provider.
 */
@Embeddable
public class LegendURL {

  /**
   * Legend width.
   */
  private Integer width;

  /**
   * Legend height.
   */
  private Integer height;

  /**
   * Legend format.
   */
  @Column(length = 80)
  private String format;

  /**
   * Legend URL.
   */
  @Column(length = 250)
  @NotNull
  private String onlineResource;

  public Integer getWidth() {
    return width;
  }

  public void setWidth(Integer width) {
    this.width = width;
  }

  public Integer getHeight() {
    return height;
  }

  public void setHeight(Integer height) {
    this.height = height;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getOnlineResource() {
    return onlineResource;
  }

  public void setOnlineResource(String onlineResource) {
    this.onlineResource = onlineResource;
  }
}

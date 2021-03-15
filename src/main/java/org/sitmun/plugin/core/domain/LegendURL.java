package org.sitmun.plugin.core.domain;

import lombok.*;
import org.sitmun.plugin.core.constraints.HttpURL;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * Legend provider.
 */
@Embeddable
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LegendURL {

  /**
   * Legend width.
   */
  @Min(1)
  private Integer width;

  /**
   * Legend height.
   */
  @Min(1)
  private Integer height;

  /**
   * Legend format.
   */
  @Column(length = IDENTIFIER)
  private String format;

  /**
   * Legend URL.
   */
  @Column(length = 250)
  @NotNull
  @HttpURL
  private String onlineResource;
}

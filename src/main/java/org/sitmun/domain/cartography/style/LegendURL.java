package org.sitmun.domain.cartography.style;

import lombok.*;
import org.sitmun.infrastructure.persistence.type.basic.Http;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import static org.sitmun.domain.PersistenceConstants.IDENTIFIER;
import static org.sitmun.domain.PersistenceConstants.URL;

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
  @Column(length = URL)
  @NotNull
  @Http
  private String onlineResource;
}

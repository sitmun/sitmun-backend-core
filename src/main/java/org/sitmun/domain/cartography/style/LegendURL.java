package org.sitmun.domain.cartography.style;

import static org.sitmun.domain.PersistenceConstants.IDENTIFIER;
import static org.sitmun.domain.PersistenceConstants.URL;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.sitmun.infrastructure.persistence.type.basic.Http;

/** Legend provider. */
@Embeddable
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LegendURL {

  /** Legend width. */
  @Min(1)
  private Integer width;

  /** Legend height. */
  @Min(1)
  private Integer height;

  /** Legend format. */
  @Column(length = IDENTIFIER)
  private String format;

  /** Legend URL. */
  @Column(length = URL)
  @NotNull
  @Http
  private String onlineResource;
}

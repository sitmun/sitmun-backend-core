package org.sitmun.infrastructure.persistence.type.envelope;

import lombok.*;
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class EnvelopeDTO {
  private Double minY;
  private Double maxX;
  private Double maxY;
  private Double minX;
}

package org.sitmun.infrastructure.persistence.type.point;

import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class PointDTO {
  private Double x;
  private Double y;
}

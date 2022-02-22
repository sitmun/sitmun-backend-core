package org.sitmun.repository.handlers.stm2;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
class BasicParameter {
  private String name;
  private String value;
  private String type;
  private Integer order;
}

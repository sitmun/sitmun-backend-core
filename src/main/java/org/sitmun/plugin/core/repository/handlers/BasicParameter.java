package org.sitmun.plugin.core.repository.handlers;

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

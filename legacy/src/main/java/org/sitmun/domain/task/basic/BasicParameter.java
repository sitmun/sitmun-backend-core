package org.sitmun.domain.task.basic;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public
class BasicParameter {
  private String name;
  private String value;
  private String type;
  private Integer order;
}

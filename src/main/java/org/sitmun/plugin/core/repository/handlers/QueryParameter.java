package org.sitmun.plugin.core.repository.handlers;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
class QueryParameter {
  private String key;
  private String type;
  private String label;
  private String select;
  private Integer order;
}

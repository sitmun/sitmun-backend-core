package org.sitmun.legacy.domain.task.query;

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
  private String value;
  private Integer order;
}

package org.sitmun.authorization.client.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ApplicationBackgroundDto {
  private String id;
  private String title;
  private String thumbnail;

  /** Application-background order (`ABC_ORDER`). */
  private Integer order;
}

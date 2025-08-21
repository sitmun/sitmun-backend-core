package org.sitmun.authorization.client.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BackgroundDto {
  private String id;
  private String title;
  private String thumbnail;
}

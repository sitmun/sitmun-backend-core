package org.sitmun.authorization.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplicationDto {
  private int id;
  private String title;
  private String type;
  private String theme;
  private String srs;
}

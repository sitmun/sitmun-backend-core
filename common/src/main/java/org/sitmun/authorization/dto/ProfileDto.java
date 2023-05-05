package org.sitmun.authorization.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProfileDto {
  private ApplicationDto application;
  private TerritoryDto territory;
  private List<BackgroundDto> backgrounds;
  private List<CartographyPermissionDto> groups;
  private List<CartographyDto> layers;
  private List<TaskDto> tasks;
}

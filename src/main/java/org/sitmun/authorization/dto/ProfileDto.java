package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class ProfileDto {
  private ApplicationDto application;
  private List<BackgroundDto> backgrounds;
  private List<CartographyPermissionDto> groups;
  private List<CartographyDto> layers;
  private List<ServiceDto> services;
  private List<TaskDto> tasks;
  private List<TreeDto> trees;
  private Map<String, String> global;
}

package org.sitmun.administration.controller.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateLinkedTaskDto {
  Integer id;
  String name;
  String scope;
  String linkedType;
  boolean hasAuthentication;
}

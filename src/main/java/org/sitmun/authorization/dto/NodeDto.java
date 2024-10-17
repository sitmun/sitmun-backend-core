package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class NodeDto {
  String title;
  String resource;
  Boolean isRadio;
  Integer order;
  List<String> children;
}

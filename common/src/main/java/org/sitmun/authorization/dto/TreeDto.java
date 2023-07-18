package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
public class TreeDto {
  private String id;
  private String title;
  private String image;
  private String rootNode;
  private Map<String, NodeDto> nodes;
}

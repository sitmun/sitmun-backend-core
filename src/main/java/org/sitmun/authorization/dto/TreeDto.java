package org.sitmun.authorization.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TreeDto {
  private String id;
  private String title;
  private String type;
  private String image;
  private String rootNode;
  private Map<String, NodeDto> nodes;
}

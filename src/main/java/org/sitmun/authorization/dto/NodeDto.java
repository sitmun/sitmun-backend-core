package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class NodeDto {
  String uri;
  String title;
  String resource;
  Boolean isRadio;
  Boolean loadData;
  Integer order;
  List<String> children;
}

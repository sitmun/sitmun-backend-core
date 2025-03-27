package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class NodeDto {
  String uri;
  String title;
  String description;
  String resource;
  String action;
  Boolean isRadio;
  Boolean loadData;
  String type;
  String viewMode;
  String image;
  Integer order;
  Map<String, Object> mapping;
  List<String> children;
}

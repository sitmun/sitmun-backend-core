package org.sitmun.authorization.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CartographyDto {
  String id;
  String title;
  List<String> layers;

  ServiceDto service;
}

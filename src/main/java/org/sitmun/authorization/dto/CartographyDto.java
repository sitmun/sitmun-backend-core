package org.sitmun.authorization.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CartographyDto {
  String id;
  String title;
  List<String> layers;
  String service;
}

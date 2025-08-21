package org.sitmun.authorization.client.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CartographyPermissionDto {
  String id;
  String title;
  List<String> layers;
}

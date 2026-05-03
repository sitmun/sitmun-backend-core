package org.sitmun.authorization.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartographyDto {
  String id;
  String title;
  List<String> layers;
  String service;
  Integer minScaleDenominator;
  Integer maxScaleDenominator;
  Integer transparency;
  String metadataURL;
  String datasetURL;
}

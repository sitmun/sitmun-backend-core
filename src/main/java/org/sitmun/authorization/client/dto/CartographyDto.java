package org.sitmun.authorization.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
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
  String description;
  List<String> layers;
  String service;
  Integer minScaleDenominator;
  Integer maxScaleDenominator;
  Integer transparency;
  Integer order;
  String metadataURL;
  String datasetURL;
  Boolean queryableFeatureEnabled;
  List<FeatureInfoFieldDto> featureInfoFields;
  String url;
  Map<String, String> parameters;
}

package org.sitmun.authorization.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureInfoFieldDto {
  String name;
  String label;
  String format;
  Integer order;
  Integer fractionDigits;
  Boolean padFractionDigits;
  String dateStyle;
}

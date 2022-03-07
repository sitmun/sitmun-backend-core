package org.sitmun.feature.admin.extractor.featuretype;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Builder
@Getter
@Setter
public class ExtractedMetadata {
  private Boolean success;
  private String reason;
  private String type;
  private String asText;
  private Map<String, Object> asJson;
}

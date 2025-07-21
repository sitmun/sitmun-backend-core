package org.sitmun.administration.service.extractor.featuretype;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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

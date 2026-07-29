package org.sitmun.administration.controller.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class TemplatePreviewRequestDto {
  private Integer templateTaskId;
  private String templateHtml;
  private Map<String, Object> context;
  private List<String> knownTaskReferences;

  /** Optional Preview coordinates so APP_* / TERR_* system variables can resolve. */
  private Integer appId;

  private Integer terId;
}

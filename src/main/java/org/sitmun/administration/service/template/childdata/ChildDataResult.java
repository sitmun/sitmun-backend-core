package org.sitmun.administration.service.template.childdata;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChildDataResult {
  ChildDataOutcome outcome;
  List<Map<String, Object>> rows;
  Map<String, Object> context;
  String resultType;
  String resourceUrl;

  public static ChildDataResult noData() {
    return ChildDataResult.builder()
        .outcome(ChildDataOutcome.NO_DATA)
        .rows(Collections.emptyList())
        .context(Collections.emptyMap())
        .build();
  }

  public static ChildDataResult unsupported(String scope) {
    return ChildDataResult.builder()
        .outcome(ChildDataOutcome.UNSUPPORTED)
        .rows(Collections.emptyList())
        .context(Collections.singletonMap("scope", scope))
        .build();
  }
}

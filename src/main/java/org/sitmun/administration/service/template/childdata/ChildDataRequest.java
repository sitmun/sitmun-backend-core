package org.sitmun.administration.service.template.childdata;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.task.Task;

@Value
@Builder
public class ChildDataRequest {
  Integer appId;
  Integer terId;
  Integer taskId;
  Map<String, String> parameters;
  PrincipalKind principalKind;
  RequestCoordinates coordinates;
  Task task;
  String scope;
}

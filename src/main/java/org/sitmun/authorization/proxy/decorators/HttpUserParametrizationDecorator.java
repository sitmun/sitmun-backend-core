package org.sitmun.authorization.proxy.decorators;

import java.util.Map;
import org.sitmun.authorization.proxy.dto.HttpPayloadDto;
import org.sitmun.authorization.proxy.dto.PayloadDto;
import org.sitmun.infrastructure.util.UriTemplateExpander;
import org.springframework.stereotype.Component;

@Component
public class HttpUserParametrizationDecorator implements Decorator<Map<String, String>> {

  @Override
  public boolean accept(Map<String, String> target, PayloadDto payload) {
    return payload instanceof HttpPayloadDto;
  }

  @Override
  public void addBehavior(Map<String, String> target, PayloadDto payload) {
    if (payload instanceof HttpPayloadDto http) {
      applyHttpParameterization(target, http);
    }
  }

  private void applyHttpParameterization(Map<String, String> target, HttpPayloadDto http) {
    if (target == null || target.isEmpty()) {
      return;
    }

    String uri = http.getUri();

    // Use UriTemplateExpander to expand {variable} in URIs
    UriTemplateExpander.ExpandedResult result =
        UriTemplateExpander.expandWithUsedVariables(uri, target);
    uri = result.getUri();

    // Remove expanded variables from parameters
    if (http.getParameters() != null) {
      result.getUsedVariables().forEach(key -> http.getParameters().remove(key));
    }

    http.setUri(uri);
  }
}

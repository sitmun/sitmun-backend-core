package org.sitmun.authorization.proxy.decorators;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    Map<String, String> payloadParameters =
        http.getParameters() != null ? new HashMap<>(http.getParameters()) : new HashMap<>();

    if ((target == null || target.isEmpty()) && payloadParameters.isEmpty()) {
      return;
    }

    // Client request parameters first; task payload parameters (defaults, provided secrets, etc.)
    // overwrite on key collision so backend configuration always wins.
    Map<String, String> combinedParameters = new LinkedHashMap<>();
    if (target != null && !target.isEmpty()) {
      combinedParameters.putAll(target);
    }
    combinedParameters.putAll(payloadParameters);

    Map<String, String> remainingParameters = new HashMap<>(combinedParameters);

    String uri = http.getUri();

    // Use UriTemplateExpander to expand {variable} in URIs
    UriTemplateExpander.ExpandedResult result =
        UriTemplateExpander.expandWithUsedVariables(uri, combinedParameters);
    uri = result.getUri();
    result.getUsedVariables().forEach(remainingParameters::remove);

    http.setUri(uri);
    http.setParameters(remainingParameters);
  }
}

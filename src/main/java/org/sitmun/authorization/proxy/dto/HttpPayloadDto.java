package org.sitmun.authorization.proxy.dto;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class HttpPayloadDto extends PayloadDto {

  private String uri;
  private String method;
  private Map<String, String> parameters;
  private HttpSecurityDto security;
  private String body;

  protected HttpPayloadDto(
      List<String> vary,
      String uri,
      String method,
      Map<String, String> parameters,
      HttpSecurityDto security,
      String body) {
    super(vary);
    this.uri = uri;
    this.method = method;
    this.parameters = parameters;
    this.security = security;
    this.body = body;
  }
}

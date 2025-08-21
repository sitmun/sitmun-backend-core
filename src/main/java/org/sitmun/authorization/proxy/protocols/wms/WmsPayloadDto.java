package org.sitmun.authorization.proxy.protocols.wms;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.authorization.proxy.dto.HttpSecurityDto;
import org.sitmun.authorization.proxy.dto.PayloadDto;

@Getter
@Setter
@JsonTypeName("OgcWmsPayload")
public class WmsPayloadDto extends PayloadDto {

  private String uri;
  private String method;
  private Map<String, String> parameters;
  private HttpSecurityDto security;
  private String body;

  @Builder
  public WmsPayloadDto(
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

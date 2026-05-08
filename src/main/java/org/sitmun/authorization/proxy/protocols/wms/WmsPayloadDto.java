package org.sitmun.authorization.proxy.protocols.wms;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.authorization.proxy.dto.HttpPayloadDto;
import org.sitmun.authorization.proxy.dto.HttpSecurityDto;

@Getter
@Setter
@JsonTypeName("OgcWmsPayload")
public class WmsPayloadDto extends HttpPayloadDto {

  @Builder
  public WmsPayloadDto(
      List<String> vary,
      String uri,
      String method,
      Map<String, String> parameters,
      HttpSecurityDto security,
      String body) {
    super(vary, uri, method, parameters, security, body);
  }
}

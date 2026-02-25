package org.sitmun.authorization.proxy.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
@Builder
public class HttpSecurityDto {

  private String type;

  private String scheme;

  private String username;

  private String password;

  private Map<String, String> headers;
}

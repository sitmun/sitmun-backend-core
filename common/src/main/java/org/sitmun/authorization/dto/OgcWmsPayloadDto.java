package org.sitmun.authorization.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonTypeName("OgcWmsPayload")
public class OgcWmsPayloadDto extends PayloadDto {

	private String uri;
	
	private String method;
	
	private Map<String, String> parameters;
	
	private HttpSecurityDto security;
}

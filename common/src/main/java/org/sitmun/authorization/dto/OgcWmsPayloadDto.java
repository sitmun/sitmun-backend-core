package org.sitmun.authorization.dto;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonTypeName("OgcWmsPayload")
public class OgcWmsPayloadDto extends PayloadDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9168137306913828620L;

	private String uri;
	
	private String method;
	
	private Map<String, String> parameters;
	
	private HttpSecurityDto security;
}

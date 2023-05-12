package org.sitmun.authorization.dto;

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class HttpSecurityDto implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8000856624562888322L;

	private String type;
	
	private String scheme;
	
	private String username;
	
	private String password;
}

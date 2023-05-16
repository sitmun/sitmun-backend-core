package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonTypeName("DatasourcePayload")
public class DatasourcePayloadDto extends PayloadDto {

	private String uri;
	
	private String user;
	
	private String password;
	
	private String driver;
	
	private String sql;
}

package org.sitmun.authorization.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonTypeName;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonTypeName("DatasourcePayload")
public class DatasourcePayloadDto extends PayloadDto implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6403427503301613899L;

	private String uri;
	
	private String user;
	
	private String password;
	
	private String driver;
	
	private String sql;
}

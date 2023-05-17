package org.sitmun.authorization.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ConfigProxyDto implements Serializable{

	private static final long serialVersionUID = -6818782288551485250L;

	private String type;
	
	private long exp;
	
	private List<String> vary;
	
	@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="type")
	@JsonSubTypes({@JsonSubTypes.Type(value = OgcWmsPayloadDto.class), @JsonSubTypes.Type(value = DatasourcePayloadDto.class)})
	private PayloadDto payload;

}
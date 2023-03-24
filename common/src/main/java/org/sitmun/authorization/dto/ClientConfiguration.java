package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.territory.Territory;

import java.util.List;

@Getter
@Setter
@Builder
@JsonView(ClientConfigurationViews.Base.class)
public class ClientConfiguration {

  List<Territory> territories;

  List<ConfigurationParameter> config;

}

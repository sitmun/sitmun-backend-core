package org.sitmun.authorization.dto;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.territory.Territory;

import java.util.List;

@Getter
@Setter
@Builder
@JsonView(ClientConfigurationViews.ApplicationTerritory.class)
public class ClientConfigurationForApplicationTerritory {

  Territory territory;

  Application application;

  List<Role> roles;

  List<ConfigurationParameter> config;

}

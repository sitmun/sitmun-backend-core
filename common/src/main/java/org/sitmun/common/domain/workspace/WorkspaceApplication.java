package org.sitmun.common.domain.workspace;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.common.domain.application.Application;
import org.sitmun.common.domain.configuration.ConfigurationParameter;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.territory.Territory;
import org.sitmun.common.views.Views;

import java.util.List;

@Getter
@Setter
@Builder
@JsonView(Views.WorkspaceApplication.class)
public class WorkspaceApplication {

  Territory territory;

  Application application;

  List<Role> roles;

  List<ConfigurationParameter> config;

}

package org.sitmun.domain;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.views.Views;

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

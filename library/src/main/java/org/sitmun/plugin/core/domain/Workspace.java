package org.sitmun.plugin.core.domain;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@JsonView(Workspace.View.class)
public class Workspace {

  List<Territory> territories;

  List<ConfigurationParameter> config;

  public static class View {
  }
}

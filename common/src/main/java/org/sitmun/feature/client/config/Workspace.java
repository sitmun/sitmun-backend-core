package org.sitmun.feature.client.config;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.common.domain.configuration.ConfigurationParameter;
import org.sitmun.common.domain.territory.Territory;

import java.util.List;

@Getter
@Setter
@Builder
@JsonView(Views.Workspace.class)
public class Workspace {

  List<Territory> territories;

  List<ConfigurationParameter> config;

}

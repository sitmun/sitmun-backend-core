package org.sitmun.authorization.client.mapper;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.sitmun.authorization.client.dto.DashboardApplicationDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationPointOfContactPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN)
public interface DashboardMapper {

  @Mapping(target = "creator", ignore = true)
  DashboardApplicationDto mapToDashboard(Application application);

  List<DashboardApplicationDto> mapToDashboard(List<Application> applications);

  @AfterMapping
  default void mapDerivedFields(Application source, @MappingTarget DashboardApplicationDto target) {
    if ("E".equals(source.getType())) {
      String url = source.getJspTemplate();
      if (url != null && !url.isBlank()) {
        target.setExternalUrl(url.trim());
      }
    }
    if (ApplicationPointOfContactPolicy.hasPublishableEmail(source.getCreator())) {
      target.setCreator(source.getCreator().getEmail());
    }
  }
}

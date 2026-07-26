package org.sitmun.authorization.client.mapper;

import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.sitmun.authorization.client.dto.ApplicationDtoLittle;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationPointOfContactPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ApplicationMapper {

  @Mapping(target = "pointOfContact", ignore = true)
  ApplicationDtoLittle map(Application application);

  List<ApplicationDtoLittle> map(List<Application> applications);

  @AfterMapping
  default void mapDerivedFields(Application source, @MappingTarget ApplicationDtoLittle target) {
    if ("E".equals(source.getType())) {
      String url = source.getJspTemplate();
      if (url != null && !url.isBlank()) {
        target.setExternalUrl(url.trim());
      }
    }
    if (ApplicationPointOfContactPolicy.hasPublishableEmail(source.getCreator())) {
      target.setPointOfContact(source.getCreator().getEmail());
    }
  }
}

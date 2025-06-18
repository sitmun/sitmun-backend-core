package org.sitmun.authorization.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.UserResolver;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = UserResolver.class)
public interface ApplicationMapper {
  List<ApplicationDtoLittle> map(List<Application> applications);

  @Mapping(source = "creatorId", target = "creator")
  ApplicationDtoLittle map(Application application);
}

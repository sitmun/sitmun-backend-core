package org.sitmun.authorization.dto;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.User;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationMapper {
  List<ApplicationDtoLittle> map(List<Application> applications);

  default String map(User user) {
    if (user == null) {
      return null;
    }
    return user.getUsername();
  }
}

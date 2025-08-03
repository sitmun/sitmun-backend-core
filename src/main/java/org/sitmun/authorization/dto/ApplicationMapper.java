package org.sitmun.authorization.dto;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ApplicationMapper {
  List<ApplicationDtoLittle> map(List<Application> applications);

  default String map(User user) {
    if (user == null) {
      return null;
    }
    return user.getUsername();
  }
}

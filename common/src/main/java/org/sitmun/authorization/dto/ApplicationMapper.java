package org.sitmun.authorization.dto;

import org.mapstruct.Mapper;
import org.sitmun.domain.application.Application;

import java.util.List;

@Mapper public interface ApplicationMapper {
  List<ApplicationDto> map (List<Application> applications);
}

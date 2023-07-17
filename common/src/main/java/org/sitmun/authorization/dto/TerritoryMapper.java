package org.sitmun.authorization.dto;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.sitmun.domain.territory.Territory;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TerritoryMapper {
  List<TerritoryDtoLittle> map(List<Territory> territories);
}

package org.sitmun.domain.user.position;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPositionMapper {
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "territory.id", target = "territoryId")
  UserPositionDTO toDto(UserPosition userPosition);

  @Mapping(source = "userId", target = "user.id")
  @Mapping(source = "territoryId", target = "territory.id")
  UserPosition toEntity(UserPositionDTO userPositionDTO);
}

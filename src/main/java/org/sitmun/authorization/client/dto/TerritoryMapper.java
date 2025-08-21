package org.sitmun.authorization.client.dto;

import java.util.List;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryDTO;
import org.sitmun.domain.territory.type.TerritoryType;
import org.sitmun.domain.territory.type.TerritoryTypeDTO;
import org.sitmun.domain.user.position.UserPositionMapper;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;
import org.sitmun.infrastructure.persistence.type.envelope.EnvelopeDTO;
import org.sitmun.infrastructure.persistence.type.point.Point;
import org.sitmun.infrastructure.persistence.type.point.PointDTO;

@Mapper(uses = UserPositionMapper.class, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TerritoryMapper {
  TerritoryMapper INSTANCE = Mappers.getMapper(TerritoryMapper.class);

  @Mapping(target = "extent", source = "extent", qualifiedByName = "mapEnvelope")
  @Mapping(target = "center", source = "center", qualifiedByName = "mapPoint")
  @Mapping(target = "type", source = "type", qualifiedByName = "mapTerritoryType")
  @Mapping(target = "positions", source = "positions")
  TerritoryDTO map(Territory territory);

  List<TerritoryDTO> map(List<Territory> territories);

  @Named("mapEnvelope")
  default EnvelopeDTO mapEnvelope(Envelope value) {
    return value != null
        ? EnvelopeDTO.builder()
            .minY(value.getMinY())
            .maxX(value.getMaxX())
            .maxY(value.getMaxY())
            .minX(value.getMinX())
            .build()
        : null;
  }

  @Named("mapPoint")
  default PointDTO mapPoint(Point value) {
    return value != null ? PointDTO.builder().x(value.getX()).y(value.getY()).build() : null;
  }

  @Named("mapTerritoryType")
  default TerritoryTypeDTO mapTerritoryType(TerritoryType value) {
    return value != null
        ? TerritoryTypeDTO.builder()
            .id(value.getId())
            .name(value.getName())
            .official(value.getOfficial())
            .topType(value.getTopType())
            .bottomType(value.getBottomType())
            .build()
        : null;
  }
}

package org.sitmun.authorization.dto;

import org.mapstruct.Mapper;
import org.sitmun.authorization.service.Profile;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;

@Mapper public interface ProfileMapper {
  ProfileDto map (Profile profile);

  default TerritoryDto map (Territory territory) {
    TerritoryDto territoryDto = new TerritoryDto();
    Envelope extent = territory.getExtent();
    territoryDto.setInitialExtent(new Double[] {
      extent.getMinX(),
      extent.getMinY(),
      extent.getMaxX(),
      extent.getMaxY()
    });
    return territoryDto;
  }

  default CartographyDto map (Cartography cartography) {
    return CartographyDto.builder()
      .id("layer/"+cartography.getId())
      .title(cartography.getName())
      .layers(cartography.getLayers())
      .service(map(cartography.getService()))
      .build();
  }

  default CartographyPermissionDto map (CartographyPermission cartographyPermission) {
    return CartographyPermissionDto.builder()
      .id("group/"+cartographyPermission.getId())
      .title(cartographyPermission.getName())
      .layers(cartographyPermission.getMembers().stream().map(it -> "layer/"+it.getId()).collect(java.util.stream.Collectors.toList()))
      .build();
  }
  default ServiceDto map (Service service) {
    return ServiceDto.builder()
      .url(service.getServiceURL())
      .type(service.getType())
      .parameters(service.getParameters().stream().map(it -> new String[] {it.getName(), it.getValue()}).collect(java.util.stream.Collectors.toMap(it -> it[0], it -> it[1])))
      .build();
  }
}

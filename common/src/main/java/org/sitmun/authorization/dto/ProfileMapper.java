package org.sitmun.authorization.dto;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.sitmun.authorization.service.Profile;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.persistence.type.envelope.Envelope;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {
  ProfileDto map(Profile profile);

  default TerritoryDto map(Territory territory) {
    TerritoryDto territoryDto = new TerritoryDto();
    Envelope extent = territory.getExtent();
    territoryDto.setInitialExtent(new Double[]{
      extent.getMinX(),
      extent.getMinY(),
      extent.getMaxX(),
      extent.getMaxY()
    });
    return territoryDto;
  }

  default CartographyDto map(Cartography cartography) {
    return CartographyDto.builder()
      .id("layer/" + cartography.getId())
      .title(cartography.getName())
      .layers(cartography.getLayers())
      .service(map(cartography.getService()))
      .build();
  }

  default CartographyPermissionDto map(CartographyPermission cartographyPermission) {
    return CartographyPermissionDto.builder()
      .id("group/" + cartographyPermission.getId())
      .title(cartographyPermission.getName())
      .layers(cartographyPermission.getMembers().stream().map(it -> "layer/" + it.getId()).collect(java.util.stream.Collectors.toList()))
      .build();
  }

  default ServiceDto map(Service service) {
    return ServiceDto.builder()
      .url(service.getServiceURL())
      .type(service.getType())
      .parameters(service.getParameters().stream().map(it -> new String[]{it.getName(), it.getValue()}).collect(java.util.stream.Collectors.toMap(it -> it[0], it -> it[1])))
      .build();
  }

  default BackgroundDto mapCartographyPermissionToBackground(CartographyPermission background) {
    return BackgroundDto.builder()
      .id("group/" + background.getId())
      .title(background.getName())
      .build();
  }

  default TaskDto map(Task task) {
    return TaskDto.builder()
      .id("task/" + task.getId())
      .uiControl(task.getUi().getName())
      .build();
  }

  @AfterMapping
  default void completeProfile(Profile profile, @MappingTarget ProfileDto profileDto) {
    Comparator<ApplicationBackground> order = Comparator.nullsLast(Comparator.comparing(ApplicationBackground::getOrder));
    List<BackgroundDto> backgrounds = profile.getApplication().getBackgrounds().stream()
      .sorted(order)
      .map(ApplicationBackground::getBackground)
      .filter(Background::getActive)
      .map(Background::getCartographyGroup)
      .map(this::mapCartographyPermissionToBackground)
      .collect(Collectors.toList());
    profileDto.setBackgrounds(backgrounds);
  }


  default String mapCartographyPermissionToString(CartographyPermission value) {
    if (value == null) return null;
    return "group/" + value.getId();
  }
}

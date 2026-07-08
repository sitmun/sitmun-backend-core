package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.client.dto.ServiceDto;
import org.sitmun.domain.service.Service;

class ProfileMapperServiceMappingTest {

  private final ProfileMapper mapper = Mappers.getMapper(ProfileMapper.class);

  @Test
  void publishesServiceTitleAndDescriptionInProfileServiceDto() {
    Service service =
        Service.builder()
            .id(166)
            .name("Published service title")
            .description("Profile service description")
            .serviceURL("https://sitmun.example/wms")
            .type("WMS")
            .isProxied(false)
            .supportedSRS(List.of("EPSG:25831"))
            .build();

    ServiceDto dto = mapper.map(service);

    assertThat(dto.getId()).isEqualTo("service/166");
    assertThat(dto.getTitle()).isEqualTo("Published service title");
    assertThat(dto.getDescription()).isEqualTo("Profile service description");
    assertThat(dto.getUrl()).isEqualTo("https://sitmun.example/wms");
  }
}

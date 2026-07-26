package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.client.dto.DashboardApplicationDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.User;

@DisplayName("DashboardMapper")
class DashboardMapperTest {

  private final DashboardMapper mapper = Mappers.getMapper(DashboardMapper.class);

  @Test
  @DisplayName("maps responsibleInstitutionName independently of creator eligibility")
  void institutionMappedIndependentlyOfCreator() {
    User blocked =
        User.builder()
            .id(5)
            .username("blocked")
            .blocked(true)
            .administrator(true)
            .email("b@b.com")
            .build();
    Application app =
        Application.builder()
            .id(1)
            .name("App")
            .type("I")
            .creator(blocked)
            .responsibleInstitutionName("ACME Corp")
            .build();

    DashboardApplicationDto dto = mapper.mapToDashboard(app);
    assertThat(dto.getResponsibleInstitutionName()).isEqualTo("ACME Corp");
    assertThat(dto.getCreator()).isNull();
  }

  @Test
  @DisplayName("maps creator email only when publishable")
  void mapsCreatorEmailWhenPublishable() {
    User eligible =
        User.builder()
            .id(42)
            .username("alice")
            .blocked(false)
            .administrator(true)
            .email("alice@example.com")
            .build();
    Application app = Application.builder().id(1).name("App").type("I").creator(eligible).build();

    assertThat(mapper.mapToDashboard(app).getCreator()).isEqualTo("alice@example.com");
  }
}

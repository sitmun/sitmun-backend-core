package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.client.dto.ApplicationDtoLittle;
import org.sitmun.domain.application.Application;

@DisplayName("ApplicationMapper")
class ApplicationMapperTest {

  private static final String IDEE_URL = "https://www.idee.es";

  private final ApplicationMapper mapper = Mappers.getMapper(ApplicationMapper.class);

  @Test
  @DisplayName("maps externalUrl for type E with jspTemplate")
  void mapsExternalUrlForExternalApp() {
    Application app =
        Application.builder().id(1).name("IDEE").type("E").jspTemplate(IDEE_URL).build();

    ApplicationDtoLittle dto = mapper.map(app);

    assertThat(dto.getExternalUrl()).isEqualTo(IDEE_URL);
  }

  @Test
  @DisplayName("does not map externalUrl for type I")
  void doesNotMapExternalUrlForInternalApp() {
    Application app =
        Application.builder().id(1).name("Internal").type("I").jspTemplate("legacy.jsp").build();

    ApplicationDtoLittle dto = mapper.map(app);

    assertThat(dto.getExternalUrl()).isNull();
  }

  @Test
  @DisplayName("does not map externalUrl for type E with blank jspTemplate")
  void doesNotMapExternalUrlWhenTemplateBlank() {
    Application app =
        Application.builder().id(1).name("External").type("E").jspTemplate(" ").build();

    ApplicationDtoLittle dto = mapper.map(app);

    assertThat(dto.getExternalUrl()).isNull();
  }
}

package org.sitmun.authorization.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.client.dto.ApplicationDtoLittle;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.User;

@DisplayName("ApplicationMapper")
class ApplicationMapperTest {

  private static final String IDEE_URL = "https://www.idee.es";

  private final ApplicationMapper mapper = Mappers.getMapper(ApplicationMapper.class);

  private User eligibleUser(String email) {
    return User.builder()
        .id(42)
        .username("alice")
        .blocked(false)
        .administrator(true)
        .email(email)
        .build();
  }

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

  @Test
  @DisplayName("maps pointOfContact when creator is eligible with non-blank email")
  void mapsPointOfContactForEligibleCreatorWithEmail() {
    Application app =
        Application.builder()
            .id(1)
            .name("App")
            .type("I")
            .creator(eligibleUser("alice@example.com"))
            .build();

    assertThat(mapper.map(app).getPointOfContact()).isEqualTo("alice@example.com");
  }

  @Test
  @DisplayName("pointOfContact is null when eligible creator has blank email")
  void nullPointOfContactWhenCreatorHasBlankEmail() {
    Application app =
        Application.builder().id(1).name("App").type("I").creator(eligibleUser(null)).build();

    assertThat(mapper.map(app).getPointOfContact()).isNull();
  }

  @Test
  @DisplayName("pointOfContact is null when creator is built-in public")
  void nullPointOfContactForPublicCreator() {
    User publicUser =
        User.builder().id(1).username("public").blocked(false).administrator(false).build();
    Application app = Application.builder().id(1).name("App").type("I").creator(publicUser).build();

    assertThat(mapper.map(app).getPointOfContact()).isNull();
  }

  @Test
  @DisplayName("pointOfContact is null when creator is blocked")
  void nullPointOfContactForBlockedCreator() {
    User blocked =
        User.builder()
            .id(5)
            .username("blocked")
            .blocked(true)
            .administrator(true)
            .email("b@b.com")
            .build();
    Application app = Application.builder().id(1).name("App").type("I").creator(blocked).build();

    assertThat(mapper.map(app).getPointOfContact()).isNull();
  }

  @Test
  @DisplayName("responsibleInstitutionName is mapped independently of creator eligibility")
  void institutionMappedIndependentlyOfCreator() {
    User blockedCreator =
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
            .creator(blockedCreator)
            .responsibleInstitutionName("ACME Corp")
            .build();

    ApplicationDtoLittle dto = mapper.map(app);
    assertThat(dto.getResponsibleInstitutionName()).isEqualTo("ACME Corp");
    assertThat(dto.getPointOfContact()).isNull();
  }
}
